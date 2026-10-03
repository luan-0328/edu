package com.educore.controller.attendance;

import com.educore.attendance.dto.BatchAttendanceRequest;
import com.educore.service.attendance.AttendanceService;
import com.educore.attendance.vo.AttendanceView;
import com.educore.common.Result;
import com.educore.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Validated @RequestMapping("/api")
public class AttendanceController {
    private final AttendanceService service;
    public AttendanceController(AttendanceService service) { this.service = service; }
    @PostMapping("/teacher/schedules/{id}/attendance")
    public Result<Void> record(@PathVariable @Positive Long id, @Valid @RequestBody BatchAttendanceRequest body,
                               @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        service.record(id, body, user); return Result.success(null, rid(request));
    }
    @GetMapping("/students/me/attendance")
    public Result<List<AttendanceView>> mine(@AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        return Result.success(service.mine(user), rid(request));
    }
    @GetMapping("/teacher/schedules/{id}/attendance")
    public Result<List<com.educore.attendance.vo.AttendanceRosterView>> roster(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest request){return Result.success(service.scheduleRoster(id,user),rid(request));}
    private String rid(HttpServletRequest request) { return String.valueOf(request.getAttribute("requestId")); }
}
