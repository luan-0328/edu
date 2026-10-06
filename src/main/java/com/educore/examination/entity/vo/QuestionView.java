package com.educore.examination.entity.vo;
import com.educore.examination.entity.enums.*;
import java.time.LocalDateTime;
public record QuestionView(Long id,Long courseId,Long teacherId,QuestionType type,String content,String optionsJson,
                           String answerJson,QuestionDifficulty difficulty,QuestionStatus status,LocalDateTime createdAt) { }
