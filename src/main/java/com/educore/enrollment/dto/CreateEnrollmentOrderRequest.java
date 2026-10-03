package com.educore.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateEnrollmentOrderRequest(@NotNull @Positive Long classId) { }
