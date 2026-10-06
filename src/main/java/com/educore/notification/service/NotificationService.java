package com.educore.notification.service;

import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.enrollment.mapper.MessageConsumeLogMapper;
import com.educore.notification.mapper.NotificationMapper;
import com.educore.notification.entity.vo.NotificationView;
import com.educore.security.AuthenticatedUser;
import com.educore.user.entity.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {
    private static final String CONSUMER = "assignment-notification-v1";
    private final NotificationMapper mapper;
    private final MessageConsumeLogMapper consumeLog;
    public NotificationService(NotificationMapper mapper, MessageConsumeLogMapper consumeLog) { this.mapper = mapper; this.consumeLog = consumeLog; }

    public List<NotificationView> list(AuthenticatedUser actor) { requireActor(actor); return mapper.listMine(actor.id()); }
    public long unread(AuthenticatedUser actor) { requireActor(actor); return mapper.unreadCount(actor.id()); }

    @Transactional
    public void markRead(Long id, AuthenticatedUser actor) {
        requireActor(actor);
        if (mapper.owns(id, actor.id()) == 0) throw new BusinessException(ApiErrorCode.NOTIFICATION_NOT_FOUND,"通知不存在",HttpStatus.NOT_FOUND);
        mapper.markRead(id, actor.id());
    }

    @Transactional
    public void consumeAssignmentPublished(Long eventId, Long assignmentId, Long classId, String title) {
        if (consumeLog.insertOnce(CONSUMER, eventId) == 0) return;
        String content = "班级作业已发布：" + title;
        for (Long studentId : mapper.enrolledStudents(classId)) mapper.insertForStudent(studentId, assignmentId, title, content);
    }

    @Transactional
    public int recoverMissingAssignmentNotifications(int limit) {
        int recovered=0;
        for(var recipient:mapper.missingAssignmentNotifications(Math.max(1,Math.min(limit,1000)))) {
            recovered+=mapper.insertForStudent(recipient.studentId(),recipient.assignmentId(),recipient.title(),"班级作业已发布："+recipient.title());
        }
        return recovered;
    }

    private void requireActor(AuthenticatedUser actor) {
        if (actor == null || actor.role() == UserRole.ADMIN) throw new BusinessException(ApiErrorCode.FORBIDDEN,"仅教师或学生可使用通知",HttpStatus.FORBIDDEN);
    }
}
