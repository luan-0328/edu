package com.educore.course.entity.dto;
import com.educore.course.entity.enums.CourseStatus;
import jakarta.validation.constraints.NotNull;
public record CourseStatusRequest(@NotNull CourseStatus status) { }
