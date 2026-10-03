package com.educore.enrollment.vo;

import com.educore.enrollment.entity.EnrollmentOrderEntity;
import com.educore.enrollment.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnrollmentOrderView(Long id, String orderNo, Long studentId, Long classId, BigDecimal amount,
                                  OrderStatus status, LocalDateTime expireAt, LocalDateTime paidAt,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static EnrollmentOrderView from(EnrollmentOrderEntity e) {
        return new EnrollmentOrderView(e.getId(), e.getOrderNo(), e.getStudentId(), e.getClassId(), e.getAmount(),
                e.getStatus(), e.getExpireAt(), e.getPaidAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
