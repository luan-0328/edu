package com.educore.dashboard.service;

import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.dashboard.entity.vo.TeacherClassProgressView;
import com.educore.dashboard.entity.vo.TeacherDashboardView;
import com.educore.assignment.mapper.AssignmentMapper;
import com.educore.examination.mapper.ExamAnswerMapper;
import com.educore.security.AuthenticatedUser;
import com.educore.schedule.service.ScheduleService;
import com.educore.teachingclass.service.TeachingClassService;
import com.educore.schedule.entity.enums.ScheduleStatus;
import com.educore.user.entity.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TeacherDashboardService {
    private final TeachingClassService classes;
    private final ScheduleService schedules;
    private final ClassAnalyticsService analytics;
    private final AssignmentMapper assignments;
    private final ExamAnswerMapper answers;

    public TeacherDashboardService(TeachingClassService classes,ScheduleService schedules,ClassAnalyticsService analytics,
                                   AssignmentMapper assignments,ExamAnswerMapper answers) {
        this.classes=classes;this.schedules=schedules;this.analytics=analytics;this.assignments=assignments;this.answers=answers;
    }

    @Transactional(readOnly=true)
    public TeacherDashboardView dashboard(AuthenticatedUser actor) {
        if(actor==null||actor.role()!=UserRole.TEACHER)
            throw new BusinessException(ApiErrorCode.FORBIDDEN,"仅教师可以查看教师工作台",HttpStatus.FORBIDDEN);
        var owned=classes.teacherClasses(actor);
        var progress=owned.stream().limit(10).map(teachingClass->{
            var metrics=analytics.get(teachingClass.id(),actor,false);
            return new TeacherClassProgressView(metrics.classId(),metrics.className(),metrics.status(),metrics.plannedLessons(),
                    metrics.completedLessons(),metrics.attendanceRate(),metrics.assignmentCompletionRate(),
                    metrics.averageScore(),metrics.examPassRate());
        }).toList();
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        var upcoming=schedules.teacherSchedules(actor).stream().filter(s->s.status()==ScheduleStatus.SCHEDULED&&!s.startTime().isBefore(now)).limit(8).toList();
        long pendingAssignments=assignments.countPendingForTeacher(actor.id());
        long pendingExamAnswers=answers.countPendingForTeacher(actor.id());
        return new TeacherDashboardView(owned.size(),(int)owned.stream().filter(c->c.status()==com.educore.teachingclass.entity.enums.ClassStatus.IN_PROGRESS).count(),
                pendingAssignments,pendingExamAnswers,upcoming,assignments.pendingForTeacher(actor.id(),8),answers.pendingByExamForTeacher(actor.id(),8),progress);
    }
}
