package com.educore.examination.entity.vo;
import com.educore.examination.entity.enums.QuestionType;
import java.math.BigDecimal;
public record ExamQuestionView(Long id,QuestionType type,String content,String optionsJson,BigDecimal score,Integer sortOrder) { }
