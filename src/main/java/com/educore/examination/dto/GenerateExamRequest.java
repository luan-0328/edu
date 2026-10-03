package com.educore.examination.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
public record GenerateExamRequest(@NotEmpty @Size(max=20) List<@Valid QuestionRule> rules) { }
