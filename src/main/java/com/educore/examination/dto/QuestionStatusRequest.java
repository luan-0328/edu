package com.educore.examination.dto;
import com.educore.examination.enums.QuestionStatus;
import jakarta.validation.constraints.NotNull;
public record QuestionStatusRequest(@NotNull QuestionStatus status) { }
