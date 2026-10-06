package com.educore.dashboard.entity.vo;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardView(long totalStudents, long teachingClassCount,
                                long currentEnrollmentCount, long paidOrderCount,
                                BigDecimal simulatedOrderAmount, long newEnrollmentsThisMonth,
                                List<PopularCourseView> popularCourses,
                                List<ClassTeachingSummaryView> classTeaching) { }
