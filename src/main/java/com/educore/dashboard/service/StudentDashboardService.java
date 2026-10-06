package com.educore.dashboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.educore.assignment.entity.vo.AssignmentView;
import com.educore.dashboard.entity.vo.NextLessonView;
import com.educore.dashboard.entity.vo.StudentDashboardView;
import com.educore.examination.entity.enums.ExamStatus;
import com.educore.examination.entity.vo.ExamSummaryView;
import com.educore.dashboard.mapper.DashboardMapper;
import com.educore.enrollment.mapper.ClassStudentMapper;
import com.educore.examination.mapper.ExamMapper;
import com.educore.schedule.mapper.ScheduleMapper;
import com.educore.schedule.entity.ScheduleEntity;
import com.educore.schedule.entity.enums.ScheduleStatus;
import com.educore.security.AuthenticatedUser;
import com.educore.assignment.service.AssignmentService;
import com.educore.examination.service.ExaminationService;
import com.educore.notification.service.NotificationService;
import com.educore.user.entity.enums.UserRole;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentDashboardService {
    private final ClassStudentMapper memberships;
    private final DashboardMapper dashboards;
    private final ScheduleMapper schedules;
    private final AssignmentService assignments;
    private final ExaminationService examinations;
    private final ExamMapper exams;
    private final NotificationService notifications;

    public StudentDashboardService(ClassStudentMapper memberships, DashboardMapper dashboards,
                                   ScheduleMapper schedules, AssignmentService assignments,
                                   ExaminationService examinations, ExamMapper exams,
                                   NotificationService notifications) {
        this.memberships=memberships; this.dashboards=dashboards; this.schedules=schedules;
        this.assignments=assignments; this.examinations=examinations; this.exams=exams;
        this.notifications=notifications;
    }

    @Transactional(readOnly=true)
    public StudentDashboardView dashboard(AuthenticatedUser actor) {
        requireStudent(actor);
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC), horizon=now.plusDays(7);
        var classes=memberships.selectStudentClasses(actor.id());
        var classIds=classes.stream().map(c->c.classId()).toList();
        NextLessonView next=null;
        if(!classIds.isEmpty()) {
            Map<Long,String> names=classes.stream().collect(Collectors.toMap(c->c.classId(),c->c.className()));
            next=schedules.selectList(Wrappers.<ScheduleEntity>lambdaQuery()
                            .in(ScheduleEntity::getClassId,classIds).eq(ScheduleEntity::getStatus,ScheduleStatus.SCHEDULED)
                            .ge(ScheduleEntity::getStartTime,now).orderByAsc(ScheduleEntity::getStartTime).last("LIMIT 1"))
                    .stream().findFirst().map(s->new NextLessonView(s.getId(),s.getClassId(),names.get(s.getClassId()),
                            s.getClassroomId(),s.getStartTime(),s.getEndTime())).orElse(null);
        }
        List<AssignmentView> due=assignments.mine(actor).stream()
                .filter(a->!a.deadline().isBefore(now)&&!a.deadline().isAfter(horizon)).toList();
        List<ExamSummaryView> upcoming=examinations.studentExams(actor).stream()
                .filter(e->e.status()==ExamStatus.PUBLISHED&&!e.endTime().isBefore(now)&&!e.startTime().isAfter(horizon)).toList();
        return new StudentDashboardView(classes,dashboards.studentClassProgress(actor.id()),next,due,upcoming,
                exams.recentStudentResults(actor.id()),notifications.unread(actor));
    }

    private void requireStudent(AuthenticatedUser actor) {
        if(actor==null||actor.role()!=UserRole.STUDENT)
            throw new BusinessException(ApiErrorCode.FORBIDDEN,"仅学生可以查看学习中心",HttpStatus.FORBIDDEN);
    }
}
