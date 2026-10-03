package com.educore.enrollment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @TableName("payment_record")
public class PaymentRecordEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String paymentNo;
    private Long orderId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
