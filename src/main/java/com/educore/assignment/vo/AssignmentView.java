package com.educore.assignment.vo;

import com.educore.assignment.enums.AssignmentStatus;
import java.time.LocalDateTime;
public record AssignmentView(Long id, Long classId, String className, String title, String content,
                             LocalDateTime deadline, AssignmentStatus status, Long submissionId,
                             String submissionContent, String submissionStatus, java.math.BigDecimal score,
                             String feedback, LocalDateTime submittedAt) { }
