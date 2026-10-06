package com.educore.examination.entity.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
public record CreateExamRequest(@NotNull @Positive Long classId,@NotBlank @Size(max=160) String title,
                                @NotNull LocalDateTime startTime,@NotNull LocalDateTime endTime,
                                @Min(1) @Max(1440) int durationMinutes) { }
