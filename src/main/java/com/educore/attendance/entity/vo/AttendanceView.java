package com.educore.attendance.entity.vo;

import com.educore.attendance.entity.enums.AttendanceStatus;
import java.time.LocalDateTime;

public record AttendanceView(Long id, Long scheduleId, Long classId, String className, Long studentId,
                             String studentName, AttendanceStatus status, LocalDateTime checkedAt) { }
