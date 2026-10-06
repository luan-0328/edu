package com.educore.assignment.entity.vo;
import com.educore.assignment.entity.enums.AssignmentStatus;
import java.time.LocalDateTime;
public record AssignmentAdminView(Long id, Long classId, Long teacherId, String title, String content,
                                  LocalDateTime deadline, AssignmentStatus status) { }
