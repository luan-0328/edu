package com.educore.dashboard.entity.vo;

import java.math.BigDecimal;
import java.util.List;

public record ClassAnalyticsView(Long classId, String className, String status,
                                 Long plannedLessons, Long completedLessons,
                                 BigDecimal attendanceRate, BigDecimal assignmentCompletionRate,
                                 BigDecimal averageScore, BigDecimal highestScore,
                                 BigDecimal lowestScore, BigDecimal examPassRate,
                                 List<ExamAnalyticsView> exams,
                                 List<StudentExamHistoryView> recentStudentResults) { }
