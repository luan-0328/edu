package com.educore.security;
import com.educore.user.entity.enums.UserRole;
public record AuthenticatedUser(Long id, String username, UserRole role) { }
