package com.educore.dashboard.entity.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentExamResultView(Long examId, String examTitle, Long classId, String className,
                                    BigDecimal score, LocalDateTime submittedAt) { }
