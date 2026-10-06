package com.educore.examination.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.educore.examination.entity.enums.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("question")
public class QuestionEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long courseId; private Long teacherId; private QuestionType type;
    private String content; private String optionsJson; private String answerJson; private QuestionDifficulty difficulty;
    private QuestionStatus status; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
