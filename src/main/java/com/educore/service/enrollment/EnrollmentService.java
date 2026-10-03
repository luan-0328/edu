package com.educore.service.enrollment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.common.PageView;
import com.educore.config.EnrollmentProperties;
import com.educore.enrollment.entity.ClassStudentEntity;
import com.educore.enrollment.entity.EnrollmentOrderEntity;
import com.educore.enrollment.entity.PaymentRecordEntity;
import com.educore.enrollment.enums.OrderStatus;
import com.educore.mapper.enrollment.ClassStudentMapper;
import com.educore.mapper.enrollment.EnrollmentOfferingMapper;
import com.educore.mapper.enrollment.EnrollmentOrderMapper;
import com.educore.mapper.enrollment.MessageConsumeLogMapper;
import com.educore.mapper.enrollment.PaymentRecordMapper;
import com.educore.enrollment.messaging.OrderTimeoutEvent;
import com.educore.enrollment.vo.*;
import com.educore.teachingclass.enums.ClassStatus;
import com.educore.service.teachingclass.ClassEnrollmentCapacityService;
import com.educore.service.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class EnrollmentService {
    private static final String TIMEOUT_CONSUMER = "order-timeout-v1";
    private final EnrollmentOrderMapper orders;
    private final ClassStudentMapper memberships;
    private final PaymentRecordMapper payments;
    private final EnrollmentOfferingMapper offerings;
    private final MessageConsumeLogMapper consumeLogs;
    private final ClassEnrollmentCapacityService capacity;
    private final UserService users;
    private final MessageOutboxService outbox;
    private final EnrollmentProperties properties;

    public EnrollmentService(EnrollmentOrderMapper orders, ClassStudentMapper memberships, PaymentRecordMapper payments,
                             EnrollmentOfferingMapper offerings, MessageConsumeLogMapper consumeLogs,
                             ClassEnrollmentCapacityService capacity, UserService users, MessageOutboxService outbox,
                             EnrollmentProperties properties) {
        this.orders = orders; this.memberships = memberships; this.payments = payments; this.offerings = offerings;
        this.consumeLogs = consumeLogs; this.capacity = capacity; this.users = users; this.outbox = outbox; this.properties = properties;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EnrollmentOrderView createOrder(Long studentId, Long classId) {
        users.lockActiveStudent(studentId); // same-student submissions serialize before checking pending orders/membership
        var now = orders.currentUtcTime();
        EnrollmentOrderEntity pending = orders.selectPending(studentId, classId);
        if (pending != null) {
            if (now.isBefore(pending.getExpireAt())) return EnrollmentOrderView.from(pending);
            pending.setStatus(OrderStatus.EXPIRED); orders.updateById(pending); capacity.release(classId);
        }
        if (memberships.selectEnrolled(classId, studentId) != null)
            throw new BusinessException(ApiErrorCode.ALREADY_ENROLLED, "已在该班级中，无需重复报名", HttpStatus.CONFLICT);

        EnrollmentOffering offer = offerings.find(classId);
        if (offer == null) throw new BusinessException(ApiErrorCode.CLASS_NOT_FOUND, "班级不存在", HttpStatus.NOT_FOUND);
        if (offer.getCourseStatus() != com.educore.course.enums.CourseStatus.PUBLISHED)
            throw new BusinessException(ApiErrorCode.INVALID_STATE, "班级课程当前未上架", HttpStatus.CONFLICT);
        if (offer.getClassStatus() != ClassStatus.ENROLLING)
            throw new BusinessException(ApiErrorCode.INVALID_STATE, "班级当前未开放报名", HttpStatus.CONFLICT);
        capacity.reserve(classId);

        EnrollmentOrderEntity order = new EnrollmentOrderEntity();
        order.setOrderNo("EC" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        order.setStudentId(studentId); order.setClassId(classId); order.setAmount(offer.getPrice());
        order.setStatus(OrderStatus.PENDING); order.setExpireAt(now.plusMinutes(Math.max(1, properties.getOrderTtlMinutes())));
        orders.insert(order);
        order = orders.selectById(order.getId());
        outbox.enqueueTimeout(order);
        return EnrollmentOrderView.from(order);
    }

    @Transactional
    public PayOrderView pay(Long studentId, Long orderId) {
        EnrollmentOrderEntity snapshot = orders.selectById(orderId);
        if (snapshot == null) throw orderNotFound();
        assertOwner(snapshot, studentId);
        EnrollmentOrderEntity order = orders.selectByIdForUpdate(orderId);
        if (order == null) throw orderNotFound();
        if (order.getStatus() == OrderStatus.PAID) {
            PaymentRecordEntity payment = payments.selectByOrderId(orderId);
            if (payment == null) throw new IllegalStateException("Paid order has no payment record: " + orderId);
            return new PayOrderView(EnrollmentOrderView.from(order), PaymentView.from(payment));
        }
        if (order.getStatus() == OrderStatus.EXPIRED)
            throw new BusinessException(ApiErrorCode.ORDER_EXPIRED, "订单已超时", HttpStatus.CONFLICT);
        if (order.getStatus() != OrderStatus.PENDING)
            throw new BusinessException(ApiErrorCode.INVALID_ORDER_STATE, "当前订单状态不能支付", HttpStatus.CONFLICT);

        var now = orders.currentUtcTime();
        if (!now.isBefore(order.getExpireAt())) {
            order.setStatus(OrderStatus.EXPIRED); orders.updateById(order); capacity.release(order.getClassId());
            return new PayOrderView(EnrollmentOrderView.from(order), null);
        }

        PaymentRecordEntity payment = new PaymentRecordEntity();
        payment.setPaymentNo("PAY" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        payment.setOrderId(orderId); payment.setAmount(order.getAmount()); payment.setStatus("SUCCESS"); payment.setPaidAt(now);
        payments.insert(payment);
        capacity.confirm(order.getClassId());
        ClassStudentEntity membership = new ClassStudentEntity();
        membership.setClassId(order.getClassId()); membership.setStudentId(studentId); membership.setStatus("ENROLLED"); membership.setEnrolledAt(now);
        memberships.insert(membership);
        order.setStatus(OrderStatus.PAID); order.setPaidAt(now); orders.updateById(order);
        return new PayOrderView(EnrollmentOrderView.from(order), PaymentView.from(payment));
    }

    @Transactional
    public EnrollmentOrderView cancel(Long studentId, Long orderId) {
        EnrollmentOrderEntity order = ownedOrderForUpdate(studentId, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED)
            return EnrollmentOrderView.from(order);
        if (order.getStatus() == OrderStatus.PAID)
            throw new BusinessException(ApiErrorCode.INVALID_ORDER_STATE, "已支付订单不能取消", HttpStatus.CONFLICT);
        if (order.getStatus() != OrderStatus.PENDING)
            throw new BusinessException(ApiErrorCode.INVALID_ORDER_STATE, "当前订单状态不能取消", HttpStatus.CONFLICT);
        if (!orders.currentUtcTime().isBefore(order.getExpireAt())) {
            order.setStatus(OrderStatus.EXPIRED); orders.updateById(order); capacity.release(order.getClassId());
            return EnrollmentOrderView.from(order);
        }
        order.setStatus(OrderStatus.CANCELLED); orders.updateById(order); capacity.release(order.getClassId());
        return EnrollmentOrderView.from(order);
    }

    @Transactional(readOnly = true)
    public EnrollmentOrderView getOrder(Long studentId, Long orderId) {
        EnrollmentOrderEntity order = orders.selectById(orderId);
        if (order == null) throw orderNotFound();
        assertOwner(order, studentId); return EnrollmentOrderView.from(order);
    }

    @Transactional(readOnly = true)
    public PageView<EnrollmentOrderView> myOrders(Long studentId, long page, long size) {
        Page<EnrollmentOrderEntity> result = orders.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<EnrollmentOrderEntity>().eq(EnrollmentOrderEntity::getStudentId, studentId)
                        .orderByDesc(EnrollmentOrderEntity::getCreatedAt).orderByDesc(EnrollmentOrderEntity::getId));
        return PageView.from(result, result.getRecords().stream().map(EnrollmentOrderView::from).toList());
    }

    @Transactional(readOnly = true)
    public PageView<EnrollmentOrderView> adminOrders(long page, long size, Long studentId, Long classId, OrderStatus status) {
        LambdaQueryWrapper<EnrollmentOrderEntity> query = new LambdaQueryWrapper<EnrollmentOrderEntity>()
                .eq(studentId != null, EnrollmentOrderEntity::getStudentId, studentId)
                .eq(classId != null, EnrollmentOrderEntity::getClassId, classId)
                .eq(status != null, EnrollmentOrderEntity::getStatus, status)
                .orderByDesc(EnrollmentOrderEntity::getCreatedAt).orderByDesc(EnrollmentOrderEntity::getId);
        Page<EnrollmentOrderEntity> result = orders.selectPage(new Page<>(page, size), query);
        return PageView.from(result, result.getRecords().stream().map(EnrollmentOrderView::from).toList());
    }

    @Transactional(readOnly = true)
    public List<StudentClassView> studentClasses(Long studentId) { return memberships.selectStudentClasses(studentId); }

    @Transactional
    public boolean expireIfDue(Long orderId) {
        EnrollmentOrderEntity order = orders.selectByIdForUpdate(orderId);
        if (order == null || order.getStatus() != OrderStatus.PENDING) return false;
        if (orders.currentUtcTime().isBefore(order.getExpireAt())) return false;
        order.setStatus(OrderStatus.EXPIRED); orders.updateById(order); capacity.release(order.getClassId());
        return true;
    }

    @Transactional
    public void consumeTimeout(OrderTimeoutEvent event) {
        if (consumeLogs.insertOnce(TIMEOUT_CONSUMER, event.eventId()) == 0) return;
        expireIfDue(event.orderId());
    }

    @Transactional(readOnly = true)
    public List<Long> overdueOrderIds(int limit) { return orders.selectOverdueIds(limit); }

    private EnrollmentOrderEntity ownedOrderForUpdate(Long studentId, Long orderId) {
        EnrollmentOrderEntity snapshot = orders.selectById(orderId);
        if (snapshot == null) throw orderNotFound();
        assertOwner(snapshot, studentId);
        EnrollmentOrderEntity order = orders.selectByIdForUpdate(orderId);
        if (order == null) throw orderNotFound();
        return order;
    }
    private void assertOwner(EnrollmentOrderEntity order, Long studentId) {
        if (!order.getStudentId().equals(studentId))
            throw new BusinessException(ApiErrorCode.ORDER_OWNER_MISMATCH, "无权访问此订单", HttpStatus.FORBIDDEN);
    }
    private BusinessException orderNotFound() { return new BusinessException(ApiErrorCode.ORDER_NOT_FOUND, "订单不存在", HttpStatus.NOT_FOUND); }
}
