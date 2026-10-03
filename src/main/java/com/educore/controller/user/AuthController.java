package com.educore.controller.user;
import com.educore.common.Result;
import com.educore.user.dto.LoginRequest;
import com.educore.user.dto.RegisterRequest;
import com.educore.service.user.UserService;
import com.educore.user.vo.LoginView;
import com.educore.user.vo.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final UserService service; public AuthController(UserService service){this.service=service;}
 @PostMapping("/register") public Result<UserView> register(@Valid @RequestBody RegisterRequest req,HttpServletRequest r){return Result.success(service.register(req),rid(r));}
 @PostMapping("/login") public Result<LoginView> login(@Valid @RequestBody LoginRequest req,HttpServletRequest r){return Result.success(service.login(req),rid(r));}
 private String rid(HttpServletRequest r){return String.valueOf(r.getAttribute("requestId"));}
}
