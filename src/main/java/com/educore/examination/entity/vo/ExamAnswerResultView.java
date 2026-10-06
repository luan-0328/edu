package com.educore.examination.entity.vo;
import com.educore.examination.entity.enums.QuestionType;
import java.math.BigDecimal;
public record ExamAnswerResultView(Long answerId,Long examQuestionId,QuestionType type,String content,
                                   String optionsJson,String answerJson,String correctAnswerJson,
                                   BigDecimal score,BigDecimal maxScore,String feedback) { }
