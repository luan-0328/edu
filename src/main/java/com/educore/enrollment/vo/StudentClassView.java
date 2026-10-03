package com.educore.enrollment.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentClassView(Long classId, Long courseId, String courseName, String className,
                               Long teacherId, LocalDate startDate, LocalDate endDate, LocalDateTime enrolledAt) { }
