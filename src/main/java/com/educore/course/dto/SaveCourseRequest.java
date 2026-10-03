package com.educore.course.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
public record SaveCourseRequest(@NotBlank @Size(max=120) String name,@Size(max=10000) String description,
 @NotNull @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal price) { }
