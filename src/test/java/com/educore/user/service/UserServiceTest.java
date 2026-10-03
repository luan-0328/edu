package com.educore.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.educore.common.BusinessException;
import com.educore.security.JwtService;
import com.educore.user.dto.LoginRequest;
import com.educore.user.dto.RegisterRequest;
import com.educore.user.entity.UserEntity;
import com.educore.user.enums.UserRole;
import com.educore.user.enums.UserStatus;
import com.educore.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private UserMapper mapper; private BCryptPasswordEncoder encoder; private UserService service;
    @BeforeEach void setUp(){mapper=mock(UserMapper.class);encoder=new BCryptPasswordEncoder();service=new UserService(mapper,encoder,mock(JwtService.class));}
    @Test void registerStoresBcryptHashAndStudentRole(){
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);when(mapper.insert(any(UserEntity.class))).thenAnswer(inv->{UserEntity u=inv.getArgument(0);u.setId(17L);return 1;});
        var view=service.register(new RegisterRequest("student_1","Password123","张三"));
        assertEquals(17L,view.id());assertEquals(UserRole.STUDENT,view.role());
        var captor=org.mockito.ArgumentCaptor.forClass(UserEntity.class);verify(mapper).insert(captor.capture());
        assertTrue(encoder.matches("Password123",captor.getValue().getPasswordHash()));assertNotEquals("Password123",captor.getValue().getPasswordHash());
    }
    @Test void loginRejectsDisabledUser(){
        UserEntity u=new UserEntity();u.setUsername("disabled");u.setPasswordHash(encoder.encode("Password123"));u.setStatus(UserStatus.DISABLED);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
        BusinessException e=assertThrows(BusinessException.class,()->service.login(new LoginRequest("disabled","Password123")));
        assertEquals("USER_DISABLED",e.getCode().name());
    }
    @Test void loginRejectsWrongPassword(){
        UserEntity u=new UserEntity();u.setUsername("student");u.setPasswordHash(encoder.encode("right-password"));u.setStatus(UserStatus.ACTIVE);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
        assertThrows(BusinessException.class,()->service.login(new LoginRequest("student","wrong-password")));
    }
}
