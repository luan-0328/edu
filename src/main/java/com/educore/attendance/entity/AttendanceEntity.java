package com.educore.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.educore.attendance.enums.AttendanceStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("attendance")
public class AttendanceEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long scheduleId;
    private Long studentId;
    private AttendanceStatus status;
    private LocalDateTime checkedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
