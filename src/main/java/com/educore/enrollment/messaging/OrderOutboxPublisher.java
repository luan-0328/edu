package com.educore.enrollment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.educore.config.messaging.RabbitMessagingConfig;
import com.educore.enrollment.entity.MessageOutboxEntity;
import com.educore.enrollment.service.MessageOutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "educore.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderOutboxPublisher.class);
    private final MessageOutboxService outbox;
    private final RabbitTemplate rabbit;
    private final ObjectMapper mapper;
    public OrderOutboxPublisher(MessageOutboxService outbox, RabbitTemplate rabbit, ObjectMapper mapper) {
        this.outbox = outbox; this.rabbit = rabbit; this.mapper = mapper;
    }

    @Scheduled(fixedDelayString = "${educore.messaging.outbox-poll-ms:5000}")
    public void publishPending() {
        for (MessageOutboxEntity row : outbox.claimBatch(50)) publish(row);
    }

    private void publish(MessageOutboxEntity row) {
        try {
            CorrelationData correlation = new CorrelationData("outbox-" + row.getId());
            String exchange = RabbitMessagingConfig.DELAY_EXCHANGE;
            String routingKey = RabbitMessagingConfig.DELAY_ROUTING_KEY;
            Object payload;
            if ("ASSIGNMENT_PUBLISHED".equals(row.getEventType())) {
                payload = mapper.readValue(row.getPayload(), AssignmentPublishedEvent.class);
                exchange = RabbitMessagingConfig.EVENT_EXCHANGE;
                routingKey = RabbitMessagingConfig.ASSIGNMENT_ROUTING_KEY;
            } else if ("ORDER_TIMEOUT".equals(row.getEventType())) {
                payload = mapper.readValue(row.getPayload(), OrderTimeoutEvent.class);
            } else {
                throw new IllegalArgumentException("Unsupported outbox event type: " + row.getEventType());
            }
            rabbit.convertAndSend(exchange, routingKey, payload, message -> {
                        message.getMessageProperties().setMessageId(String.valueOf(row.getId()));
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    }, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirm.isAck()) throw new IllegalStateException("Broker rejected message: " + confirm.getReason());
            if (correlation.getReturned() != null) throw new IllegalStateException("Message was returned as unroutable");
            outbox.markSent(row.getId());
        } catch (Exception e) {
            log.warn("Outbox event {} publish failed; it will be retried", row.getId(), e);
            outbox.markRetry(row.getId(), row.getAttempts(), e.getMessage());
        }
    }
}
