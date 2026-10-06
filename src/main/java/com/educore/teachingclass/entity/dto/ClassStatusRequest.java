package com.educore.teachingclass.entity.dto;
import com.educore.teachingclass.entity.enums.ClassStatus;
import jakarta.validation.constraints.NotNull;
public record ClassStatusRequest(@NotNull ClassStatus status) { }
