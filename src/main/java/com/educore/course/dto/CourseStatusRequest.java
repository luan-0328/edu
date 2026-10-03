package com.educore.course.dto;
import com.educore.course.enums.CourseStatus;
import jakarta.validation.constraints.NotNull;
public record CourseStatusRequest(@NotNull CourseStatus status) { }
