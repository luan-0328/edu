package com.educore.examination.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.educore.examination.entity.enums.ExamStatus;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("exam")
public class ExamEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long classId; private Long teacherId; private String title;
    private LocalDateTime startTime; private LocalDateTime endTime; private Integer durationMinutes;
    private ExamStatus status; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
