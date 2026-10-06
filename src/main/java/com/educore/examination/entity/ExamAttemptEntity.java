package com.educore.examination.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.educore.examination.entity.enums.AttemptStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("exam_attempt")
public class ExamAttemptEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long examId; private Long studentId; private AttemptStatus status;
    private LocalDateTime startedAt; private LocalDateTime submittedAt; private BigDecimal score; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
