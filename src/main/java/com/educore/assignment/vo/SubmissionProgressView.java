package com.educore.assignment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
public record SubmissionProgressView(Long studentId, String studentName, String status, String content,
                                     BigDecimal score, String feedback, LocalDateTime submittedAt, LocalDateTime gradedAt) { }
