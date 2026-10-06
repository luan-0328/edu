package com.educore.audit.controller;

import com.educore.audit.entity.OperationAuditLogEntity;
import com.educore.audit.service.OperationAuditService;
import com.educore.common.PageView;
import com.educore.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin/audit-logs")
public class OperationAuditController {
    private final OperationAuditService service;
    public OperationAuditController(OperationAuditService service){this.service=service;}

    @GetMapping
    public Result<PageView<OperationAuditLogEntity>> list(@RequestParam(defaultValue="1") @Min(1) long page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) long size,
            @RequestParam(required=false) @Min(1) Long actorId,@RequestParam(required=false) String outcome,
            HttpServletRequest request) {
        return Result.success(service.list(page,size,actorId,outcome),String.valueOf(request.getAttribute("requestId")));
    }
}
