package com.educore.dashboard.controller;

import com.educore.common.Result;
import com.educore.dashboard.entity.vo.ClassAnalyticsView;
import com.educore.security.AuthenticatedUser;
import com.educore.dashboard.service.ClassAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class ClassAnalyticsController {
    private final ClassAnalyticsService service;
    public ClassAnalyticsController(ClassAnalyticsService service){this.service=service;}

    @GetMapping("/admin/classes/{classId}/analytics")
    public Result<ClassAnalyticsView> admin(@PathVariable @Positive Long classId,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest request){
        return Result.success(service.get(classId,user,true),String.valueOf(request.getAttribute("requestId")));
    }

    @GetMapping("/teacher/classes/{classId}/analytics")
    public Result<ClassAnalyticsView> teacher(@PathVariable @Positive Long classId,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest request){
        return Result.success(service.get(classId,user,false),String.valueOf(request.getAttribute("requestId")));
    }
}
