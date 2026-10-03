package com.educore.examination.vo;
import com.educore.examination.enums.ExamStatus;
import java.time.LocalDateTime;
public record ExamAdminView(Long id,Long classId,Long teacherId,String title,LocalDateTime startTime,LocalDateTime endTime,
                            Integer durationMinutes,ExamStatus status,int questionCount) { }
