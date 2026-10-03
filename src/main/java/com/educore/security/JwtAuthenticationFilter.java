package com.educore.security;

import com.educore.user.entity.UserEntity;
import com.educore.user.enums.UserStatus;
import com.educore.user.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt; private final UserService users;
    public JwtAuthenticationFilter(JwtService jwt,UserService users){this.jwt=jwt;this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String header=request.getHeader("Authorization");
        if(header!=null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication()==null){
            try {
                Long id=jwt.userId(header.substring(7)); UserEntity user=users.requireActiveUser(id);
                if(user.getStatus()==UserStatus.ACTIVE){
                    AuthenticatedUser principal=new AuthenticatedUser(user.getId(),user.getUsername(),user.getRole());
                    var auth=new UsernamePasswordAuthenticationToken(principal,null,List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch(JwtException|IllegalArgumentException ignored) { SecurityContextHolder.clearContext(); }
              catch(RuntimeException ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(request,response);
    }
}
