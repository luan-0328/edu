package com.educore.attendance.vo;

import com.educore.attendance.enums.AttendanceStatus;
import java.time.LocalDateTime;

public record AttendanceView(Long id, Long scheduleId, Long classId, String className, Long studentId,
                             String studentName, AttendanceStatus status, LocalDateTime checkedAt) { }
