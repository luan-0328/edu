package com.educore.dashboard.entity.vo;

import java.math.BigDecimal;

public record StudentClassProgressView(Long classId, String className, String courseName,
                                       Long plannedLessons, Long completedLessons,
                                       BigDecimal attendanceRate) { }
