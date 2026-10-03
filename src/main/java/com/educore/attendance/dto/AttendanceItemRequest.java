package com.educore.attendance.dto;

import com.educore.attendance.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AttendanceItemRequest(@NotNull @Positive Long studentId, @NotNull AttendanceStatus status) { }
