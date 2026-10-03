package com.educore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.educore.common.ApiErrorCode;
import com.educore.common.RequestIdFilter;
import com.educore.common.Result;
import com.educore.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableWebSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,JwtAuthenticationFilter jwtFilter,RequestIdFilter requestIdFilter,ObjectMapper mapper)throws Exception{
        http.csrf(csrf->csrf.disable()).formLogin(form->form.disable()).httpBasic(basic->basic.disable())
                .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth->auth.requestMatchers("/api/auth/register","/api/auth/login","/swagger-ui.html","/swagger-ui/**","/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated())
                .exceptionHandling(errors->errors.authenticationEntryPoint((request,response,ex)->writeError(request,response,mapper,ApiErrorCode.UNAUTHENTICATED,"请先登录",401))
                        .accessDeniedHandler((request,response,ex)->writeError(request,response,mapper,ApiErrorCode.FORBIDDEN,"无权执行此操作",403)))
                .addFilterBefore(requestIdFilter,UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    private static void writeError(jakarta.servlet.http.HttpServletRequest request,HttpServletResponse response,ObjectMapper mapper,ApiErrorCode code,String message,int status)throws java.io.IOException{
        Object id=request.getAttribute(RequestIdFilter.ATTRIBUTE);response.setStatus(status);response.setContentType(MediaType.APPLICATION_JSON_VALUE);response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(),Result.failure(code,message,id==null?"unknown":id.toString()));
    }
}
