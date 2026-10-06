package com.educore.enrollment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.educore.enrollment.entity.enums.OrderStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @TableName("enrollment_order")
public class EnrollmentOrderEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String orderNo;
    private Long studentId;
    private Long classId;
    private BigDecimal amount;
    private OrderStatus status;
    private LocalDateTime expireAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
