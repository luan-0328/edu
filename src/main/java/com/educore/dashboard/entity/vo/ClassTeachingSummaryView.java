package com.educore.dashboard.entity.vo;

public record ClassTeachingSummaryView(Long classId, String className, String status,
                                       Long enrolledCount, Long plannedLessons,
                                       Long completedLessons, java.math.BigDecimal attendanceRate) { }
