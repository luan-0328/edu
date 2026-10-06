package com.educore.enrollment.messaging;

import com.educore.enrollment.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "educore.enrollment", name = "expiration-enabled", havingValue = "true", matchIfMissing = true)
public class OrderExpirationJob {
    private static final Logger log = LoggerFactory.getLogger(OrderExpirationJob.class);
    private final EnrollmentService service;
    public OrderExpirationJob(EnrollmentService service) { this.service = service; }

    @Scheduled(fixedDelayString = "${educore.enrollment.expiration-scan-ms:60000}")
    public void releaseOverdueReservations() {
        for (Long id : service.overdueOrderIds(100)) {
            try { service.expireIfDue(id); }
            catch (RuntimeException e) { log.error("Could not expire enrollment order {}", id, e); }
        }
    }
}
