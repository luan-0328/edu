package com.educore.examination.vo;
import com.educore.examination.enums.ExamStatus;
import java.time.LocalDateTime;
public record ExamSummaryView(Long id,Long classId,String className,String title,LocalDateTime startTime,
                              LocalDateTime endTime,Integer durationMinutes,ExamStatus status) { }
