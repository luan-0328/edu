package com.educore.user.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educore.common.Result;
import com.educore.security.AuthenticatedUser;
import com.educore.user.dto.CreateTeacherRequest;
import com.educore.user.dto.UserStatusRequest;
import com.educore.user.service.UserService;
import com.educore.user.vo.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Validated @RestController @RequestMapping("/api/admin")
public class AdminUserController {
 private final UserService service; public AdminUserController(UserService service){this.service=service;}
 @PostMapping("/teachers") public Result<UserView> teacher(@Valid @RequestBody CreateTeacherRequest req,HttpServletRequest r){return Result.success(service.createTeacher(req),rid(r));}
 @GetMapping("/users") public Result<Page<UserView>> users(@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(100) long size,HttpServletRequest r){return Result.success(service.listUsers(page,size),rid(r));}
 @PatchMapping("/users/{id}/status") public Result<UserView> status(@PathVariable @Min(1) Long id,@Valid @RequestBody UserStatusRequest req,@AuthenticationPrincipal AuthenticatedUser actor,HttpServletRequest r){return Result.success(service.updateStatus(id,req.status(),actor.id()),rid(r));}
 private String rid(HttpServletRequest r){return String.valueOf(r.getAttribute("requestId"));}
}
