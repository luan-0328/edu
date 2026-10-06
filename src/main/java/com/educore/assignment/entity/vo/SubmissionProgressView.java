package com.educore.assignment.entity.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
public record SubmissionProgressView(Long studentId, String studentName, Long submissionId, String status, String content,
                                     BigDecimal score, String feedback, LocalDateTime submittedAt, LocalDateTime gradedAt) { }
