package com.educore.schedule.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
public record SaveScheduleRequest(@NotNull @Positive Long classId,@NotNull @Positive Long classroomId,
 @NotNull LocalDateTime startTime,@NotNull LocalDateTime endTime) { }
