package com.educore.enrollment.entity.vo;

import com.educore.enrollment.entity.PaymentRecordEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentView(String paymentNo, Long orderId, BigDecimal amount, String status, LocalDateTime paidAt) {
    public static PaymentView from(PaymentRecordEntity e) {
        return new PaymentView(e.getPaymentNo(), e.getOrderId(), e.getAmount(), e.getStatus(), e.getPaidAt());
    }
}
