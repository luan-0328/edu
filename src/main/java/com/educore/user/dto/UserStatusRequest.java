package com.educore.user.dto;
import com.educore.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
public record UserStatusRequest(@NotNull UserStatus status) { }
