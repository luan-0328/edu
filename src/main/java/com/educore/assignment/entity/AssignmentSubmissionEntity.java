package com.educore.assignment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.educore.assignment.enums.SubmissionStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("assignment_submission")
public class AssignmentSubmissionEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long assignmentId; private Long studentId; private String content; private SubmissionStatus status;
    private BigDecimal score; private String feedback; private LocalDateTime submittedAt; private LocalDateTime gradedAt;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
