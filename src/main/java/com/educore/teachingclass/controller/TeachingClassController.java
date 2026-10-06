package com.educore.teachingclass.controller;
import com.educore.common.Result;
import com.educore.security.AuthenticatedUser;
import com.educore.teachingclass.entity.dto.SaveClassRequest;
import com.educore.teachingclass.entity.dto.ClassStatusRequest;
import com.educore.teachingclass.service.TeachingClassService;
import com.educore.teachingclass.entity.vo.TeachingClassView;
import com.educore.enrollment.entity.vo.ClassMemberView;
import com.educore.common.PageView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api") public class TeachingClassController {
 private final TeachingClassService service;public TeachingClassController(TeachingClassService service){this.service=service;}
 @PostMapping("/admin/classes") public Result<TeachingClassView> create(@Valid @RequestBody SaveClassRequest req,HttpServletRequest r){return Result.success(service.create(req),rid(r));}
 @PutMapping("/admin/classes/{id}") public Result<TeachingClassView> update(@PathVariable @Min(1) Long id,@Valid @RequestBody SaveClassRequest req,HttpServletRequest r){return Result.success(service.update(id,req),rid(r));}
 @PatchMapping("/admin/classes/{id}/status") public Result<TeachingClassView> status(@PathVariable @Min(1) Long id,@Valid @RequestBody ClassStatusRequest req,HttpServletRequest r){return Result.success(service.updateStatus(id,req.status()),rid(r));}
 @GetMapping("/classes/{id}") public Result<TeachingClassView> detail(@PathVariable @Min(1) Long id,HttpServletRequest r){return Result.success(service.detail(id),rid(r));}
 @GetMapping("/classes") public Result<List<TeachingClassView>> openByCourse(@RequestParam @Min(1) Long courseId,HttpServletRequest r){return Result.success(service.openByCourse(courseId),rid(r));}
 @GetMapping("/admin/classes") public Result<PageView<TeachingClassView>> adminList(@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(100) long size,HttpServletRequest r){return Result.success(service.listAdmin(page,size),rid(r));}
 @GetMapping("/teacher/classes") public Result<List<TeachingClassView>> mine(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.teacherClasses(user),rid(r));}
 @GetMapping("/teacher/classes/{id}/students") public Result<List<ClassMemberView>> students(@PathVariable @Min(1) Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.students(id,user),rid(r));}
 private String rid(HttpServletRequest r){return String.valueOf(r.getAttribute("requestId"));}
}
