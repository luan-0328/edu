package com.educore.attendance.vo;
import com.educore.attendance.enums.AttendanceStatus;
import java.time.LocalDateTime;
public record AttendanceRosterView(Long studentId,String realName,AttendanceStatus status,LocalDateTime checkedAt) { }
