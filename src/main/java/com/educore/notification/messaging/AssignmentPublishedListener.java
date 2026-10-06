package com.educore.notification.messaging;

import com.educore.enrollment.messaging.AssignmentPublishedEvent;
import com.educore.config.messaging.RabbitMessagingConfig;
import com.educore.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="educore.messaging", name="enabled", havingValue="true", matchIfMissing=true)
public class AssignmentPublishedListener {
    private final NotificationService service;
    public AssignmentPublishedListener(NotificationService service) { this.service = service; }
    @RabbitListener(queues=RabbitMessagingConfig.ASSIGNMENT_QUEUE)
    public void consume(AssignmentPublishedEvent event) {
        service.consumeAssignmentPublished(event.eventId(),event.assignmentId(),event.classId(),event.title());
    }
}
