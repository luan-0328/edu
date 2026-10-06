package com.educore.examination.entity.dto;
import com.educore.examination.entity.enums.QuestionStatus;
import jakarta.validation.constraints.NotNull;
public record QuestionStatusRequest(@NotNull QuestionStatus status) { }
