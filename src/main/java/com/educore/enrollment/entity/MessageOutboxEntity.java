package com.educore.enrollment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @TableName("message_outbox")
public class MessageOutboxEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String eventType;
    private String aggregateType;
    private Long aggregateId;
    private String dedupeKey;
    private String payload;
    private String status;
    private Integer attempts;
    private LocalDateTime availableAt;
    private LocalDateTime lockedUntil;
    private LocalDateTime publishedAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
