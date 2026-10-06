package com.educore.schedule.controller;
import com.educore.common.Result;
import com.educore.schedule.entity.dto.SaveScheduleRequest;
import com.educore.schedule.service.ScheduleService;
import com.educore.schedule.entity.vo.ScheduleView;
import com.educore.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @Validated @RequestMapping("/api") public class ScheduleController {
 private final ScheduleService service;public ScheduleController(ScheduleService service){this.service=service;}
 @PostMapping("/admin/schedules") public Result<ScheduleView> create(@Valid @RequestBody SaveScheduleRequest req,HttpServletRequest r){return Result.success(service.create(req),rid(r));}
 @PutMapping("/admin/schedules/{id}") public Result<ScheduleView> update(@PathVariable @Min(1) Long id,@Valid @RequestBody SaveScheduleRequest req,HttpServletRequest r){return Result.success(service.update(id,req),rid(r));}
 @GetMapping("/classes/{id}/schedules") public Result<List<ScheduleView>> classSchedules(@PathVariable @Min(1) Long id,HttpServletRequest r){return Result.success(service.classSchedules(id),rid(r));}
 @GetMapping("/teacher/schedules") public Result<List<ScheduleView>> teacherSchedules(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.teacherSchedules(user),rid(r));}
 @PostMapping("/teacher/schedules/{id}/complete") public Result<ScheduleView> complete(@PathVariable @Min(1) Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.complete(id,user),rid(r));}
 private String rid(HttpServletRequest r){return String.valueOf(r.getAttribute("requestId"));}
}
