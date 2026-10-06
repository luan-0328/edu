package com.educore.notification.entity.vo;
import java.time.LocalDateTime;
public record NotificationView(Long id, String sourceType, Long sourceId, String title, String content,
                               LocalDateTime readAt, LocalDateTime createdAt) { }
