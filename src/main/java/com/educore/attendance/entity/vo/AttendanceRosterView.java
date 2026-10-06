package com.educore.attendance.entity.vo;
import com.educore.attendance.entity.enums.AttendanceStatus;
import java.time.LocalDateTime;
public record AttendanceRosterView(Long studentId,String realName,AttendanceStatus status,LocalDateTime checkedAt) { }
