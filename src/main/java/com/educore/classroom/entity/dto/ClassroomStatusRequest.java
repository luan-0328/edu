package com.educore.classroom.entity.dto;
import com.educore.classroom.entity.enums.ClassroomStatus;
import jakarta.validation.constraints.NotNull;
public record ClassroomStatusRequest(@NotNull ClassroomStatus status) { }
