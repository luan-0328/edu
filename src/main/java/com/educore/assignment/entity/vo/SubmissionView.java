package com.educore.assignment.entity.vo;
import com.educore.assignment.entity.enums.SubmissionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record SubmissionView(Long id, Long assignmentId, Long studentId, String content, SubmissionStatus status,
                             BigDecimal score, String feedback, LocalDateTime submittedAt, LocalDateTime gradedAt) { }
