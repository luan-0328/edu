package com.educore.examination.entity.vo;
import com.educore.examination.entity.enums.ExamStatus;
import com.educore.examination.entity.enums.AttemptStatus;
import java.time.LocalDateTime;
public record ExamSummaryView(Long id,Long classId,String className,String title,LocalDateTime startTime,
                              LocalDateTime endTime,Integer durationMinutes,ExamStatus status,
                              AttemptStatus attemptStatus) { }
