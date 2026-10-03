package com.educore.controller.enrollment;

import com.educore.common.PageView;
import com.educore.common.Result;
import com.educore.enrollment.enums.OrderStatus;
import com.educore.service.enrollment.EnrollmentService;
import com.educore.enrollment.vo.EnrollmentOrderView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated @RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final EnrollmentService service;
    public AdminOrderController(EnrollmentService service) { this.service = service; }
    @GetMapping
    public Result<PageView<EnrollmentOrderView>> list(@RequestParam(defaultValue="1") @Min(1) long page,
                                                       @RequestParam(defaultValue="20") @Min(1) @Max(100) long size,
                                                       @RequestParam(required=false) @Min(1) Long studentId,
                                                       @RequestParam(required=false) @Min(1) Long classId,
                                                       @RequestParam(required=false) OrderStatus status,
                                                       HttpServletRequest http) {
        return Result.success(service.adminOrders(page, size, studentId, classId, status), String.valueOf(http.getAttribute("requestId")));
    }
}
