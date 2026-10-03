package com.educore.assignment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.educore.assignment.enums.AssignmentStatus;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("assignment")
public class AssignmentEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long classId; private Long teacherId; private String title; private String content;
    private LocalDateTime deadline; private AssignmentStatus status; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
