package com.educore.enrollment.controller;

import com.educore.common.PageView;
import com.educore.common.Result;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.enrollment.entity.dto.CreateEnrollmentOrderRequest;
import com.educore.enrollment.service.EnrollmentService;
import com.educore.enrollment.entity.vo.*;
import com.educore.security.AuthenticatedUser;
import com.educore.user.entity.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Validated @RequestMapping("/api")
public class EnrollmentController {
    private final EnrollmentService service;
    public EnrollmentController(EnrollmentService service) { this.service = service; }

    @PostMapping("/enrollments/orders")
    public Result<EnrollmentOrderView> create(@AuthenticationPrincipal AuthenticatedUser actor,
                                              @Valid @RequestBody CreateEnrollmentOrderRequest request,
                                              HttpServletRequest http) {
        return Result.success(service.createOrder(actor.id(), request.classId()), rid(http));
    }
    @GetMapping("/orders")
    public Result<PageView<EnrollmentOrderView>> mine(@AuthenticationPrincipal AuthenticatedUser actor,
                                                       @RequestParam(defaultValue="1") @Min(1) long page,
                                                       @RequestParam(defaultValue="20") @Min(1) @Max(100) long size,
                                                       HttpServletRequest http) {
        requireStudent(actor); return Result.success(service.myOrders(actor.id(), page, size), rid(http));
    }
    @GetMapping("/orders/{id}")
    public Result<EnrollmentOrderView> detail(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable @Min(1) Long id, HttpServletRequest http) {
        requireStudent(actor); return Result.success(service.getOrder(actor.id(), id), rid(http));
    }
    @PostMapping("/orders/{id}/pay")
    public Result<PayOrderView> pay(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable @Min(1) Long id, HttpServletRequest http) {
        requireStudent(actor);
        PayOrderView result = service.pay(actor.id(), id);
        if (result.order().status() == com.educore.enrollment.entity.enums.OrderStatus.EXPIRED)
            throw new BusinessException(ApiErrorCode.ORDER_EXPIRED, "订单已超时", org.springframework.http.HttpStatus.CONFLICT);
        return Result.success(result, rid(http));
    }
    @PostMapping("/orders/{id}/cancel")
    public Result<EnrollmentOrderView> cancel(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable @Min(1) Long id, HttpServletRequest http) {
        requireStudent(actor); return Result.success(service.cancel(actor.id(), id), rid(http));
    }
    @GetMapping("/students/me/classes")
    public Result<List<StudentClassView>> classes(@AuthenticationPrincipal AuthenticatedUser actor, HttpServletRequest http) {
        requireStudent(actor); return Result.success(service.studentClasses(actor.id()), rid(http));
    }

    @GetMapping("/students/me/course-enrollments")
    public Result<List<CourseEnrollmentView>> courseEnrollments(@AuthenticationPrincipal AuthenticatedUser actor, HttpServletRequest http) {
        requireStudent(actor); return Result.success(service.courseEnrollments(actor.id()), rid(http));
    }

    private void requireStudent(AuthenticatedUser actor) {
        if (actor.role() != UserRole.STUDENT) throw new org.springframework.security.access.AccessDeniedException("仅学生可以访问");
    }
    private String rid(HttpServletRequest request) { return String.valueOf(request.getAttribute("requestId")); }
}
