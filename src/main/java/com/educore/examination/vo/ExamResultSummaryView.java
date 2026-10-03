package com.educore.examination.vo;
import com.educore.examination.enums.AttemptStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record ExamResultSummaryView(Long attemptId,Long studentId,String studentName,AttemptStatus status,
                                    BigDecimal score,LocalDateTime submittedAt) { }
