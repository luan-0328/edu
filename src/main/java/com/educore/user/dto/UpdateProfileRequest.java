package com.educore.user.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record UpdateProfileRequest(@NotBlank @Size(max=64) String realName) { }
