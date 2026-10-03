package com.educore.security;

import com.educore.config.JwtProperties;
import com.educore.user.entity.UserEntity;
import com.educore.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
 @Test void tokenContainsUserIdAndCanBeParsed(){JwtService service=new JwtService(new JwtProperties("12345678901234567890123456789012",3600));UserEntity u=new UserEntity();u.setId(42L);u.setRole(UserRole.TEACHER);String token=service.issue(u);assertEquals(42L,service.userId(token));}
 @Test void rejectsShortSigningSecret(){assertThrows(IllegalStateException.class,()->new JwtService(new JwtProperties("short",3600)));}
}
