package com.educore.examination.vo;
import com.educore.examination.enums.AttemptStatus;
import java.time.LocalDateTime;
import java.util.List;
public record ExamStartView(Long examId,Long attemptId,AttemptStatus status,LocalDateTime startedAt,
                            LocalDateTime deadline,List<ExamQuestionView> questions) { }
