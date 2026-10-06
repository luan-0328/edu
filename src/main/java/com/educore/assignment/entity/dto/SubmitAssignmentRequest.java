package com.educore.assignment.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record SubmitAssignmentRequest(@NotBlank @Size(max=20000) String content) { }
