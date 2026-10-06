package com.educore.dashboard.entity.vo;

import java.math.BigDecimal;

public record ExamAnalyticsView(Long examId, String title, Long gradedAttemptCount,
                                BigDecimal averageScore, BigDecimal highestScore,
                                BigDecimal lowestScore, BigDecimal passRate) { }
