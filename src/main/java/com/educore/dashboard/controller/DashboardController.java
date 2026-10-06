package com.educore.dashboard.controller;

import com.educore.common.Result;
import com.educore.dashboard.entity.vo.AdminDashboardView;
import com.educore.dashboard.entity.vo.StudentDashboardView;
import com.educore.dashboard.entity.vo.TeacherDashboardView;
import com.educore.security.AuthenticatedUser;
import com.educore.dashboard.service.AdminDashboardService;
import com.educore.dashboard.service.StudentDashboardService;
import com.educore.dashboard.service.TeacherDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final StudentDashboardService students;
    private final AdminDashboardService admins;
    private final TeacherDashboardService teachers;
    public DashboardController(StudentDashboardService students,AdminDashboardService admins,TeacherDashboardService teachers){this.students=students;this.admins=admins;this.teachers=teachers;}

    @GetMapping("/students/me/dashboard")
    public Result<StudentDashboardView> student(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest request){
        return Result.success(students.dashboard(user),rid(request));
    }

    @GetMapping("/admin/dashboard")
    public Result<AdminDashboardView> admin(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest request){
        return Result.success(admins.dashboard(user),rid(request));
    }

    @GetMapping("/teacher/dashboard")
    public Result<TeacherDashboardView> teacher(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest request){
        return Result.success(teachers.dashboard(user),rid(request));
    }

    private String rid(HttpServletRequest request){return String.valueOf(request.getAttribute("requestId"));}
}
