package com.educore.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("notification")
public class NotificationEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long userId; private String sourceType; private Long sourceId; private String title; private String content;
    private LocalDateTime readAt; private LocalDateTime createdAt;
}
