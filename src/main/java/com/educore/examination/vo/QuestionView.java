package com.educore.examination.vo;
import com.educore.examination.enums.*;
import java.time.LocalDateTime;
public record QuestionView(Long id,Long courseId,Long teacherId,QuestionType type,String content,String optionsJson,
                           String answerJson,QuestionDifficulty difficulty,QuestionStatus status,LocalDateTime createdAt) { }
