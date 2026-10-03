package com.educore.controller.user;
import com.educore.common.Result;
import com.educore.security.AuthenticatedUser;
import com.educore.user.dto.UpdateProfileRequest;
import com.educore.service.user.UserService;
import com.educore.user.vo.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/users")
public class UserController {
 private final UserService service; public UserController(UserService service){this.service=service;}
 @GetMapping("/me") public Result<UserView> me(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.getCurrent(user.id()),rid(r));}
 @PutMapping("/me") public Result<UserView> update(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody UpdateProfileRequest req,HttpServletRequest r){return Result.success(service.updateProfile(user.id(),req),rid(r));}
 private String rid(HttpServletRequest r){return String.valueOf(r.getAttribute("requestId"));}
}
