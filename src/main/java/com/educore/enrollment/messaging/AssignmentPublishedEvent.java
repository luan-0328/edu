package com.educore.enrollment.messaging;
public record AssignmentPublishedEvent(Long eventId, Long assignmentId, Long classId, String title) { }
