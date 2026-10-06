package com.educore.assignment.entity.vo;

import com.educore.assignment.entity.enums.AssignmentStatus;
import java.time.LocalDateTime;
public record AssignmentView(Long id, Long classId, String className, String title, String content,
                             LocalDateTime deadline, AssignmentStatus status, Long submissionId,
                             String submissionContent, String submissionStatus, java.math.BigDecimal score,
                             String feedback, LocalDateTime submittedAt) { }
