package com.educore.user.entity.dto;
import com.educore.user.entity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
public record UserStatusRequest(@NotNull UserStatus status) { }
