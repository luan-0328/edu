package com.educore.examination.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("exam_answer")
public class ExamAnswerEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long attemptId; private Long examQuestionId; private String answerJson;
    private BigDecimal score; private String feedback; private LocalDateTime gradedAt; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
