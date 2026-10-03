package com.educore.examination.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record GradeExamAnswerRequest(@NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal score,@Size(max=4000) String feedback) { }
