package com.educore.classroom.entity.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record SaveClassroomRequest(@NotBlank @Size(max=120) String name,@Min(1) @Max(100000) int capacity) { }
