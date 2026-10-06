package com.educore.assignment.controller;

import com.educore.assignment.entity.dto.*;
import com.educore.assignment.entity.*;
import com.educore.assignment.service.AssignmentService;
import com.educore.assignment.entity.vo.*;
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
public class AssignmentController {
    private final AssignmentService service;
    public AssignmentController(AssignmentService service) { this.service = service; }
    @PostMapping("/teacher/assignments")
    public Result<AssignmentAdminView> create(@Valid @RequestBody SaveAssignmentRequest b,@AuthenticationPrincipal AuthenticatedUser u,HttpServletRequest r){return Result.success(service.create(b,u),rid(r));}
    @PutMapping("/teacher/assignments/{id}") public Result<AssignmentAdminView> update(@PathVariable @Positive Long id,@Valid @RequestBody SaveAssignmentRequest b,@AuthenticationPrincipal AuthenticatedUser u,HttpServletRequest r){return Result.success(service.update(id,b,u),rid(r));}
    @PostMapping("/teacher/assignments/{id}/publish") public Result<AssignmentAdminView> publish(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser u,HttpServletRequest r){return Result.success(service.publish(id,u),rid(r));}
    @PostMapping("/teacher/assignments/{id}/close") public Result<AssignmentAdminView> close(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser u,HttpServletRequest r){return Result.success(service.close(id,u),rid(r));}
    @GetMapping("/students/me/assignments") public Result<List<AssignmentView>> mine(@AuthenticationPrincipal AuthenticatedUser u,HttpServletRequest r){return Result.success(service.mine(u),rid(r));}
    @GetMapping("/teacher/classes/{classId}/assignments") public Result<List<AssignmentAdminView>> classAssignments(@PathVariable @Positive Long classId,@AuthenticationPrincipal AuthenticatedUser u,HttpServletRequest r){return Result.success(service.classAssignments(classId,u),rid(r));}
    @PutMapping("/assignments/{id}/submission") public Result<SubmissionView> submit(@PathVariable @Positive Long id,@Valid @RequestBody SubmitAssignmentRequest b,@AuthenticationPrincipal AuthenticatedUser u,HttpServletRequest r){return Result.success(service.submit(id,b,u),rid(r));}
    @GetMapping("/teacher/assignments/{id}/submissions") public Result<List<SubmissionProgressView>> progress(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser u,HttpServletRequest r){return Result.success(service.progress(id,u),rid(r));}
    @PostMapping("/teacher/submissions/{id}/grade") public Result<SubmissionView> grade(@PathVariable @Positive Long id,@Valid @RequestBody GradeSubmissionRequest b,@AuthenticationPrincipal AuthenticatedUser u,HttpServletRequest r){return Result.success(service.grade(id,b,u),rid(r));}
    private String rid(HttpServletRequest r){return String.valueOf(r.getAttribute("requestId"));}
}
