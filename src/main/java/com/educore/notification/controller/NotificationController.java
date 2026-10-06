package com.educore.notification.controller;

import com.educore.common.Result;
import com.educore.notification.service.NotificationService;
import com.educore.notification.entity.vo.NotificationView;
import com.educore.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Validated @RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public Result<List<NotificationView>> list(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.list(user),rid(r));}
    @GetMapping("/unread-count") public Result<Long> unread(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.unread(user),rid(r));}
    @PatchMapping("/{id}/read") public Result<Void> read(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){service.markRead(id,user);return Result.success(null,rid(r));}
    private String rid(HttpServletRequest r){return String.valueOf(r.getAttribute("requestId"));}
}
