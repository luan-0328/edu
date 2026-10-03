package com.educore.examination.vo;
import com.educore.examination.enums.QuestionType;
import java.math.BigDecimal;
public record PendingExamAnswerView(Long answerId,Long attemptId,Long studentId,String studentName,
                                    Long examQuestionId,QuestionType type,String content,String answerJson,BigDecimal maxScore) { }
