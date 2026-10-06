package com.educore.schedule.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.educore.schedule.entity.enums.ScheduleStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @TableName("class_schedule")
public class ScheduleEntity {
 @TableId(type=IdType.AUTO) private Long id;
 private Long classId;
 private Long teacherId;
 private Long classroomId;
 private LocalDateTime startTime;
 private LocalDateTime endTime;
 private ScheduleStatus status;
 @TableField(fill=FieldFill.INSERT) private LocalDateTime createdAt;
 @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
