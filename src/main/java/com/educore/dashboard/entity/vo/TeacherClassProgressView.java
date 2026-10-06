package com.educore.dashboard.entity.vo;

import java.math.BigDecimal;

public record TeacherClassProgressView(Long classId,String className,String status,Long plannedLessons,
                                       Long completedLessons,BigDecimal attendanceRate,
                                       BigDecimal assignmentCompletionRate,BigDecimal averageScore,
                                       BigDecimal examPassRate) { }
