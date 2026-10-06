package com.educore.attendance.entity.dto;

import com.educore.attendance.entity.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AttendanceItemRequest(@NotNull @Positive Long studentId, @NotNull AttendanceStatus status) { }
