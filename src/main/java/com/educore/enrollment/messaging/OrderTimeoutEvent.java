package com.educore.enrollment.messaging;

import java.time.LocalDateTime;

public record OrderTimeoutEvent(Long eventId, Long orderId, String orderNo, LocalDateTime expireAt) { }
