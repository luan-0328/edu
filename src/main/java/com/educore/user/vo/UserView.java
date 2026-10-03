package com.educore.user.vo;
import com.educore.user.enums.UserRole;
import com.educore.user.enums.UserStatus;
import com.educore.user.entity.UserEntity;
import java.time.LocalDateTime;
public record UserView(Long id, String username, String realName, UserRole role, UserStatus status, LocalDateTime createdAt) {
 public static UserView from(UserEntity user){return new UserView(user.getId(),user.getUsername(),user.getRealName(),user.getRole(),user.getStatus(),user.getCreatedAt());}
}
