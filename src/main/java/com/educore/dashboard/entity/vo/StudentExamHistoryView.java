package com.educore.dashboard.entity.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentExamHistoryView(Long studentId, String studentName, Long examId,
                                     String examTitle, BigDecimal score,
                                     LocalDateTime submittedAt) { }
