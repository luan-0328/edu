package com.educore.examination.entity.vo;
import com.educore.examination.entity.enums.AttemptStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
public record ExamStartView(Long examId,Long attemptId,AttemptStatus status,LocalDateTime startedAt,
                            LocalDateTime deadline,List<ExamQuestionView> questions,Map<Long,JsonNode> savedAnswers) { }
