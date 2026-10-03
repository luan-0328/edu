package com.educore.examination.vo;
import com.educore.examination.enums.AttemptStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record ExamResultView(Long examId,Long attemptId,Long studentId,String examTitle,AttemptStatus status,
                             BigDecimal score,LocalDateTime submittedAt,List<ExamAnswerResultView> answers) { }
