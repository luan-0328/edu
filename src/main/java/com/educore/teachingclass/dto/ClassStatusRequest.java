package com.educore.teachingclass.dto;
import com.educore.teachingclass.enums.ClassStatus;
import jakarta.validation.constraints.NotNull;
public record ClassStatusRequest(@NotNull ClassStatus status) { }
