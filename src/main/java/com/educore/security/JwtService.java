package com.educore.security;

import com.educore.config.JwtProperties;
import com.educore.user.entity.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties properties; private final SecretKey key;
    public JwtService(JwtProperties properties) {
        this.properties=properties;
        if (properties.secret()==null || properties.secret().getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        if(properties.ttlSeconds()<60) throw new IllegalStateException("JWT_TTL_SECONDS must be at least 60");
        key=Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }
    public String issue(UserEntity user) {
        Instant now=Instant.now();
        return Jwts.builder().subject(user.getId().toString()).claim("uid",user.getId()).claim("role",user.getRole().name())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(properties.ttlSeconds()))).signWith(key).compact();
    }
    public Long userId(String token) { return Long.valueOf(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject()); }
    public long ttlSeconds(){return properties.ttlSeconds();}
}
