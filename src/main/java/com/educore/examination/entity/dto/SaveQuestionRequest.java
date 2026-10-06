package com.educore.examination.entity.dto;
import com.educore.examination.entity.enums.*;
import jakarta.validation.constraints.*;
public record SaveQuestionRequest(@NotNull @Positive Long courseId,@NotNull QuestionType type,@NotBlank @Size(max=10000) String content,
                                  @Size(max=10000) String optionsJson,@NotBlank @Size(max=4000) String answerJson,
                                  @NotNull QuestionDifficulty difficulty) { }
