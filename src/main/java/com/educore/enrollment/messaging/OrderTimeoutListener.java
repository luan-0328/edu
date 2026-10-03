package com.educore.enrollment.messaging;

import com.educore.config.messaging.RabbitMessagingConfig;
import com.educore.service.enrollment.EnrollmentService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "educore.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderTimeoutListener {
    private final EnrollmentService service;
    public OrderTimeoutListener(EnrollmentService service) { this.service = service; }
    @RabbitListener(queues = RabbitMessagingConfig.TIMEOUT_QUEUE)
    public void onTimeout(OrderTimeoutEvent event, Message message) { service.consumeTimeout(event); }
}
