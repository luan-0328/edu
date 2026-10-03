package com.educore.examination.messaging;

import com.educore.examination.service.ExaminationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="educore.exam",name="expiration-enabled",havingValue="true",matchIfMissing=true)
public class ExamExpirationJob {
    private static final Logger log=LoggerFactory.getLogger(ExamExpirationJob.class);
    private final ExaminationService service;
    public ExamExpirationJob(ExaminationService service){this.service=service;}
    @Scheduled(fixedDelayString="${educore.exam.expiration-scan-ms:60000}")
    public void submitExpiredAttempts(){try{service.expireOverdueAttempts(100);}catch(RuntimeException e){log.error("Could not auto-submit expired exam attempts",e);}}
}
