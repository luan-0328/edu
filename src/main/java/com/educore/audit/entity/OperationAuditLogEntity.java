package com.educore.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("operation_audit_log")
public class OperationAuditLogEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long actorId;
    private String actorRole;
    private String httpMethod;
    private String requestPath;
    private Integer responseStatus;
    private String outcome;
    private String requestId;
    private LocalDateTime createdAt;
}
