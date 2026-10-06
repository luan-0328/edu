package com.educore.examination.entity.vo;
import com.educore.examination.entity.enums.ExamStatus;
import java.time.LocalDateTime;
public record ExamAdminView(Long id,Long classId,Long teacherId,String title,LocalDateTime startTime,LocalDateTime endTime,
                            Integer durationMinutes,ExamStatus status,int questionCount) { }
