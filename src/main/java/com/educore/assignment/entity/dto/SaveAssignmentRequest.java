package com.educore.assignment.entity.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record SaveAssignmentRequest(@NotNull @Positive Long classId, @NotBlank @Size(max=160) String title,
                                   @NotBlank @Size(max=10000) String content, @NotNull LocalDateTime deadline) { }
