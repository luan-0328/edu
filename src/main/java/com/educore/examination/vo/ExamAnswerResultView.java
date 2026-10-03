package com.educore.examination.vo;
import com.educore.examination.enums.QuestionType;
import java.math.BigDecimal;
public record ExamAnswerResultView(Long answerId,Long examQuestionId,QuestionType type,String content,
                                   String optionsJson,String answerJson,String correctAnswerJson,
                                   BigDecimal score,BigDecimal maxScore,String feedback) { }
