package com.educore.dashboard.entity.vo;

import java.time.LocalDateTime;

public record NextLessonView(Long scheduleId, Long classId, String className, Long classroomId,
                             LocalDateTime startTime, LocalDateTime endTime) { }
