package com.educore.dashboard.entity.vo;

import java.time.LocalDateTime;

public record TeacherPendingAssignmentView(Long assignmentId,Long classId,String className,String title,
                                          Long studentId,String studentName,LocalDateTime submittedAt) { }
