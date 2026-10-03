package com.educore.service.enrollment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.educore.enrollment.entity.EnrollmentOrderEntity;
import com.educore.enrollment.entity.MessageOutboxEntity;
import com.educore.mapper.enrollment.MessageOutboxMapper;
import com.educore.enrollment.messaging.OrderTimeoutEvent;
import com.educore.enrollment.messaging.AssignmentPublishedEvent;
import com.educore.assignment.entity.AssignmentEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class MessageOutboxService {
    private final MessageOutboxMapper mapper;
    private final ObjectMapper objectMapper;
    public MessageOutboxService(MessageOutboxMapper mapper, ObjectMapper objectMapper) { this.mapper = mapper; this.objectMapper = objectMapper; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueTimeout(EnrollmentOrderEntity order) {
        MessageOutboxEntity row = new MessageOutboxEntity();
        row.setEventType("ORDER_TIMEOUT"); row.setAggregateType("ENROLLMENT_ORDER"); row.setAggregateId(order.getId());
        row.setDedupeKey("order-timeout:" + order.getId()); row.setPayload("{}"); row.setStatus("PENDING"); row.setAttempts(0);
        mapper.insert(row);
        try {
            row.setPayload(objectMapper.writeValueAsString(new OrderTimeoutEvent(row.getId(), order.getId(), order.getOrderNo(), order.getExpireAt())));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize order timeout event", e);
        }
        mapper.updateById(row);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueAssignmentPublished(AssignmentEntity assignment) {
        MessageOutboxEntity row = new MessageOutboxEntity();
        row.setEventType("ASSIGNMENT_PUBLISHED"); row.setAggregateType("ASSIGNMENT"); row.setAggregateId(assignment.getId());
        row.setDedupeKey("assignment-published:" + assignment.getId()); row.setPayload("{}"); row.setStatus("PENDING"); row.setAttempts(0);
        mapper.insert(row);
        try {
            row.setPayload(objectMapper.writeValueAsString(new AssignmentPublishedEvent(row.getId(), assignment.getId(), assignment.getClassId(), assignment.getTitle())));
        } catch (JsonProcessingException e) { throw new IllegalStateException("Could not serialize assignment event", e); }
        mapper.updateById(row);
    }

    @Transactional
    public List<MessageOutboxEntity> claimBatch(int limit) {
        List<MessageOutboxEntity> rows = mapper.claimable(limit);
        for (MessageOutboxEntity row : rows) mapper.markProcessing(row.getId());
        rows.forEach(row -> row.setAttempts(row.getAttempts() + 1));
        return rows;
    }

    @Transactional
    public void markSent(Long id) { mapper.markSent(id); }

    @Transactional
    public void markRetry(Long id, int attempts, String error) {
        int delay = (int) Math.min(300, Math.pow(2, Math.min(Math.max(attempts, 1), 8)));
        String safeError = error == null ? "unknown publish failure" : error.substring(0, Math.min(error.length(), 900));
        mapper.markRetry(id, delay, safeError);
    }
}
