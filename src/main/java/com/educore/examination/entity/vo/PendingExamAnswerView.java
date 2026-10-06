package com.educore.examination.entity.vo;
import com.educore.examination.entity.enums.QuestionType;
import java.math.BigDecimal;
public record PendingExamAnswerView(Long answerId,Long attemptId,Long studentId,String studentName,
                                    Long examQuestionId,QuestionType type,String content,String answerJson,BigDecimal maxScore) { }
