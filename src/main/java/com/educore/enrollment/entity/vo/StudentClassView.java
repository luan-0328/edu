package com.educore.enrollment.entity.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentClassView(Long classId, Long courseId, String courseName, String className,
                               Long teacherId, String teacherName, LocalDate startDate, LocalDate endDate, LocalDateTime enrolledAt) { }
