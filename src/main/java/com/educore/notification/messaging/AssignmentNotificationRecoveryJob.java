package com.educore.notification.messaging;

import com.educore.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="educore.messaging",name="enabled",havingValue="true",matchIfMissing=true)
public class AssignmentNotificationRecoveryJob {
    private static final Logger log=LoggerFactory.getLogger(AssignmentNotificationRecoveryJob.class);
    private final NotificationService notifications;
    public AssignmentNotificationRecoveryJob(NotificationService notifications){this.notifications=notifications;}

    @Scheduled(fixedDelayString="${educore.messaging.notification-reconcile-ms:60000}")
    public void recoverMissingNotifications(){
        try{int recovered=notifications.recoverMissingAssignmentNotifications(200);if(recovered>0)log.info("Recovered {} missing assignment notifications",recovered);}
        catch(RuntimeException e){log.error("Could not recover missing assignment notifications",e);}
    }
}
