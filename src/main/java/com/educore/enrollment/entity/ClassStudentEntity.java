package com.educore.enrollment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @TableName("class_student")
public class ClassStudentEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long classId;
    private Long studentId;
    private String status;
    private LocalDateTime enrolledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
