package com.educore.classroom.dto;
import com.educore.classroom.enums.ClassroomStatus;
import jakarta.validation.constraints.NotNull;
public record ClassroomStatusRequest(@NotNull ClassroomStatus status) { }
