package com.educore.examination.vo;
import com.educore.examination.enums.QuestionType;
import java.math.BigDecimal;
public record ExamQuestionView(Long id,QuestionType type,String content,String optionsJson,BigDecimal score,Integer sortOrder) { }
