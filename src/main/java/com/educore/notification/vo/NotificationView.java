package com.educore.notification.vo;
import java.time.LocalDateTime;
public record NotificationView(Long id, String sourceType, Long sourceId, String title, String content,
                               LocalDateTime readAt, LocalDateTime createdAt) { }
