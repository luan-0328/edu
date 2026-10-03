package com.educore.assignment.vo;
import com.educore.assignment.enums.AssignmentStatus;
import java.time.LocalDateTime;
public record AssignmentAdminView(Long id, Long classId, Long teacherId, String title, String content,
                                  LocalDateTime deadline, AssignmentStatus status) { }
