package com.educore.examination.dto;
import com.educore.examination.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record QuestionRule(@NotNull QuestionType type,@NotNull QuestionDifficulty difficulty,@Min(1) @Max(100) int count,
                           @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal score) { }
