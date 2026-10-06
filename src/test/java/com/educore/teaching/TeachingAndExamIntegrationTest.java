package com.educore.teaching;

import com.educore.assignment.entity.dto.*;
import com.educore.assignment.service.AssignmentService;
import com.educore.schedule.service.ScheduleService;
import com.educore.teachingclass.service.TeachingClassService;
import com.educore.dashboard.service.StudentDashboardService;
import com.educore.dashboard.service.AdminDashboardService;
import com.educore.dashboard.service.ClassAnalyticsService;
import com.educore.attendance.entity.dto.*;
import com.educore.attendance.entity.enums.AttendanceStatus;
import com.educore.attendance.service.AttendanceService;
import com.educore.common.BusinessException;
import com.educore.examination.entity.dto.*;
import com.educore.examination.entity.enums.*;
import com.educore.examination.service.ExaminationService;
import com.educore.notification.service.NotificationService;
import com.educore.security.AuthenticatedUser;
import com.educore.security.JwtService;
import com.educore.user.entity.UserEntity;
import com.educore.user.entity.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:educore-phase45;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.datasource.hikari.connection-init-sql=","spring.flyway.enabled=false","spring.sql.init.mode=always",
 "spring.sql.init.schema-locations=classpath:schema.sql","educore.jwt.secret=phase-four-five-integration-test-secret-32-bytes-minimum",
 "educore.messaging.enabled=false","educore.enrollment.expiration-enabled=false","educore.exam.expiration-enabled=false","educore.cache.enabled=false"
})
@AutoConfigureMockMvc
class TeachingAndExamIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AttendanceService attendance;
    @Autowired ScheduleService schedules;
    @Autowired TeachingClassService classes;
    @Autowired StudentDashboardService studentDashboard;
    @Autowired AdminDashboardService adminDashboard;
    @Autowired ClassAnalyticsService classAnalytics;
    @Autowired AssignmentService assignments;
    @Autowired NotificationService notifications;
    @Autowired ExaminationService examinations;
    @Autowired com.educore.dashboard.service.TeacherDashboardService teacherDashboard;
    @Autowired ObjectMapper json;
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach void seed() {
        jdbc.update("DELETE FROM operation_audit_log"); jdbc.update("DELETE FROM course_cache_invalidation");
        jdbc.update("DELETE FROM exam_answer"); jdbc.update("DELETE FROM exam_attempt"); jdbc.update("DELETE FROM exam_question");
        jdbc.update("DELETE FROM exam"); jdbc.update("DELETE FROM question"); jdbc.update("DELETE FROM notification");
        jdbc.update("DELETE FROM message_consume_log"); jdbc.update("DELETE FROM message_outbox");
        jdbc.update("DELETE FROM assignment_submission"); jdbc.update("DELETE FROM assignment"); jdbc.update("DELETE FROM attendance");
        jdbc.update("DELETE FROM class_student"); jdbc.update("DELETE FROM class_schedule"); jdbc.update("DELETE FROM edu_class");
        jdbc.update("DELETE FROM classroom"); jdbc.update("DELETE FROM course"); jdbc.update("DELETE FROM sys_user");
        jdbc.update("INSERT INTO sys_user(id,username,password_hash,real_name,role,status) VALUES(10,'admin','hash','Admin','ADMIN','ACTIVE'),(11,'teach-a','hash','Teacher A','TEACHER','ACTIVE'),(12,'teach-b','hash','Teacher B','TEACHER','ACTIVE'),(21,'student-a','hash','Student A','STUDENT','ACTIVE'),(22,'student-b','hash','Student B','STUDENT','ACTIVE'),(23,'student-outside','hash','Outside Student','STUDENT','ACTIVE')");
        jdbc.update("INSERT INTO course(id,name,description,price,status) VALUES(31,'Java','backend',99,'PUBLISHED')");
        jdbc.update("INSERT INTO classroom(id,name,capacity,status) VALUES(41,'Room',30,'ACTIVE')");
        LocalDateTime start=LocalDateTime.now(ZoneOffset.UTC).minusHours(1),end=start.plusHours(1);
        jdbc.update("INSERT INTO edu_class(id,course_id,teacher_id,name,capacity,reserved_count,enrolled_count,start_date,end_date,status) VALUES(51,31,11,'Java A',20,0,2,?,?, 'IN_PROGRESS'),(52,31,12,'Java B',20,0,0,?,?, 'IN_PROGRESS')",start.toLocalDate(),end.toLocalDate(),start.toLocalDate(),end.toLocalDate());
        jdbc.update("INSERT INTO class_schedule(id,class_id,teacher_id,classroom_id,start_time,end_time,status) VALUES(61,51,11,41,?,?,'SCHEDULED')",start,end);
        jdbc.update("INSERT INTO class_student(class_id,student_id,status) VALUES(51,21,'ENROLLED'),(51,22,'ENROLLED')");
    }

    @Test void attendanceIsTeacherScopedIdempotentAndStudentsCannotChooseOtherClassMembers() {
        var teacher=actor(11,UserRole.TEACHER);
        var payload=new BatchAttendanceRequest(List.of(new AttendanceItemRequest(21L,AttendanceStatus.PRESENT),new AttendanceItemRequest(22L,AttendanceStatus.LATE)));
        attendance.record(61L,payload,teacher); attendance.record(61L,payload,teacher);
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM attendance WHERE schedule_id=61",Integer.class));
        assertEquals("LATE",jdbc.queryForObject("SELECT status FROM attendance WHERE schedule_id=61 AND student_id=22",String.class));
        assertForbidden(()->attendance.record(61L,payload,actor(12,UserRole.TEACHER)));
        assertForbidden(()->attendance.record(61L,new BatchAttendanceRequest(List.of(new AttendanceItemRequest(23L,AttendanceStatus.ABSENT))),teacher));
        assertEquals(1,attendance.mine(actor(21,UserRole.STUDENT)).size());
    }

    @Test void lessonCompletionRequiresFullAttendanceAndLocksAttendanceAfterward(){
        var teacher=actor(11,UserRole.TEACHER);
        attendance.record(61L,new BatchAttendanceRequest(List.of(new AttendanceItemRequest(21L,AttendanceStatus.PRESENT))),teacher);
        assertInvalid(()->schedules.complete(61L,teacher));
        attendance.record(61L,new BatchAttendanceRequest(List.of(new AttendanceItemRequest(22L,AttendanceStatus.LATE))),teacher);
        assertEquals("COMPLETED",schedules.complete(61L,teacher).status().name());
        assertInvalid(()->attendance.record(61L,new BatchAttendanceRequest(List.of(new AttendanceItemRequest(21L,AttendanceStatus.ABSENT))),teacher));
        jdbc.update("UPDATE edu_class SET start_date=?,end_date=? WHERE id=51",LocalDateTime.now(ZoneOffset.UTC).minusDays(3).toLocalDate(),LocalDateTime.now(ZoneOffset.UTC).minusDays(1).toLocalDate());
        assertEquals("FINISHED",classes.updateStatus(51L,com.educore.teachingclass.entity.enums.ClassStatus.FINISHED).status().name());
    }

    @Test void classFinishWaitsForAssignmentsExamsAndOutstandingGrades() throws Exception {
        var teacher=actor(11,UserRole.TEACHER);var student=actor(21,UserRole.STUDENT);
        attendance.record(61L,new BatchAttendanceRequest(List.of(new AttendanceItemRequest(21L,AttendanceStatus.PRESENT),new AttendanceItemRequest(22L,AttendanceStatus.LATE))),teacher);
        schedules.complete(61L,teacher);
        jdbc.update("UPDATE edu_class SET start_date=?,end_date=? WHERE id=51",LocalDateTime.now(ZoneOffset.UTC).minusDays(3).toLocalDate(),LocalDateTime.now(ZoneOffset.UTC).minusDays(1).toLocalDate());
        var assignment=assignments.create(new SaveAssignmentRequest(51L,"Final task","Complete and review",LocalDateTime.now(ZoneOffset.UTC).plusHours(2)),teacher);
        assignments.publish(assignment.id(),teacher);
        var submittedTask=assignments.submit(assignment.id(),new SubmitAssignmentRequest("Final work"),student);
        assertInvalid(()->classes.updateStatus(51L,com.educore.teachingclass.entity.enums.ClassStatus.FINISHED));
        assignments.close(assignment.id(),teacher);
        assertInvalid(()->classes.updateStatus(51L,com.educore.teachingclass.entity.enums.ClassStatus.FINISHED));
        assignments.grade(submittedTask.id(),new GradeSubmissionRequest(new BigDecimal("90"),"complete"),teacher);

        examinations.createQuestion(new SaveQuestionRequest(31L,QuestionType.SHORT_ANSWER,"Explain the topic",null,"\"answer\"",QuestionDifficulty.EASY),teacher);
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        var exam=examinations.createExam(new CreateExamRequest(51L,"Final exam",now.minusMinutes(1),now.plusHours(1),30),teacher);
        examinations.generate(exam.id(),new GenerateExamRequest(List.of(new QuestionRule(QuestionType.SHORT_ANSWER,QuestionDifficulty.EASY,1,new BigDecimal("100")))),teacher);
        examinations.publish(exam.id(),teacher);
        assertInvalid(()->classes.updateStatus(51L,com.educore.teachingclass.entity.enums.ClassStatus.FINISHED));
        var attempt=examinations.start(exam.id(),student);
        examinations.submit(exam.id(),new SubmitExamRequest(Map.of(attempt.questions().get(0).id(),json.readTree("\"answer\""))),student);
        examinations.close(exam.id(),teacher);
        assertInvalid(()->classes.updateStatus(51L,com.educore.teachingclass.entity.enums.ClassStatus.FINISHED));
        Long answerId=jdbc.queryForObject("SELECT id FROM exam_answer WHERE attempt_id=?",Long.class,attempt.attemptId());
        examinations.gradeAnswer(answerId,new GradeExamAnswerRequest(new BigDecimal("92"),"complete"),teacher);
        assertEquals("FINISHED",classes.updateStatus(51L,com.educore.teachingclass.entity.enums.ClassStatus.FINISHED).status().name());
        assertInvalid(()->assignments.create(new SaveAssignmentRequest(51L,"After finish","not allowed",now.plusHours(2)),teacher));
        assertInvalid(()->examinations.createExam(new CreateExamRequest(51L,"After finish",now,now.plusHours(1),30),teacher));
    }

    @Test void attendanceWriteWaitsForConcurrentLessonCompletion() throws Exception {
        var teacher=actor(11,UserRole.TEACHER);
        attendance.record(61L,new BatchAttendanceRequest(List.of(new AttendanceItemRequest(21L,AttendanceStatus.PRESENT),new AttendanceItemRequest(22L,AttendanceStatus.LATE))),teacher);
        var pool=Executors.newFixedThreadPool(2);var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var recordStarted=new CountDownLatch(1);
        var tx=new TransactionTemplate(transactionManager);
        try{
            var completion=pool.submit(()->tx.execute(status->{jdbc.queryForObject("SELECT id FROM class_schedule WHERE id=61 FOR UPDATE",Long.class);locked.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("test lock was not released");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}return schedules.complete(61L,teacher);}));
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            var lateAttendance=pool.submit(()->{recordStarted.countDown();attendance.record(61L,new BatchAttendanceRequest(List.of(new AttendanceItemRequest(21L,AttendanceStatus.ABSENT))),teacher);});
            assertTrue(recordStarted.await(5,TimeUnit.SECONDS));
            try{lateAttendance.get(150,TimeUnit.MILLISECONDS);fail("attendance write should wait for the schedule lock");}catch(TimeoutException expected){}
            release.countDown();
            assertEquals("COMPLETED",completion.get(5,TimeUnit.SECONDS).status().name());
            ExecutionException rejected=assertThrows(ExecutionException.class,()->lateAttendance.get(5,TimeUnit.SECONDS));
            assertInstanceOf(BusinessException.class,rejected.getCause());
            assertEquals("PRESENT",jdbc.queryForObject("SELECT status FROM attendance WHERE schedule_id=61 AND student_id=21",String.class));
        }finally{release.countDown();pool.shutdownNow();}
    }

    @Test void dashboardsAndClassAnalyticsReturnScopedAggregates() throws Exception {
        var student=studentDashboard.dashboard(actor(21,UserRole.STUDENT));
        assertEquals(1,student.enrolledClasses().size());
        assertEquals(1,student.classProgress().getFirst().plannedLessons());
        jdbc.update("INSERT INTO edu_class(id,course_id,teacher_id,name,capacity,reserved_count,enrolled_count,start_date,end_date,status) VALUES(53,31,11,'Historic class',5,0,1,DATE '2025-01-01',DATE '2025-02-01','FINISHED')");
        jdbc.update("INSERT INTO class_student(class_id,student_id,status) VALUES(53,23,'ENROLLED')");
        jdbc.update("UPDATE class_schedule SET start_time=?,end_time=? WHERE id=61",LocalDateTime.now(ZoneOffset.UTC).plusDays(1),LocalDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(1));
        assertNotNull(studentDashboard.dashboard(actor(21,UserRole.STUDENT)).nextLesson());
        var admin=adminDashboard.dashboard(actor(10,UserRole.ADMIN));
        assertEquals(3,admin.totalStudents());
        assertEquals(2,admin.currentEnrollmentCount());
        assertEquals(2,admin.popularCourses().getFirst().enrollmentCount());
        assertEquals(0,admin.simulatedOrderAmount().compareTo(BigDecimal.ZERO));
        var teacher=classAnalytics.get(51L,actor(11,UserRole.TEACHER),false);
        assertEquals(1,teacher.plannedLessons());
        assertEquals(0,teacher.assignmentCompletionRate().compareTo(BigDecimal.ZERO));
        assertForbidden(()->classAnalytics.get(51L,actor(12,UserRole.TEACHER),false));
        mvc.perform(get("/api/students/me/dashboard").header("Authorization","Bearer "+token(21,UserRole.STUDENT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.classProgress.length()").value(1));
        mvc.perform(get("/api/admin/dashboard").header("Authorization","Bearer "+token(11,UserRole.TEACHER)))
                .andExpect(status().isForbidden());
    }

    @Test void examPassRateUsesSnapshotTotalAndExcludesUnfinishedGrading() {
        var teacher=actor(11,UserRole.TEACHER);
        examinations.createQuestion(new SaveQuestionRequest(31L,QuestionType.SINGLE_CHOICE,"Choose A",
                "[{\"id\":\"A\",\"text\":\"A\"},{\"id\":\"B\",\"text\":\"B\"}]","\"A\"",QuestionDifficulty.EASY),teacher);
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        for(int total:List.of(50,100)) {
            var exam=examinations.createExam(new CreateExamRequest(51L,"Total "+total,now.minusMinutes(1),now.plusHours(1),30),teacher);
            examinations.generate(exam.id(),new GenerateExamRequest(List.of(new QuestionRule(QuestionType.SINGLE_CHOICE,QuestionDifficulty.EASY,1,BigDecimal.valueOf(total)))),teacher);
            int threshold=total*60/100;
            jdbc.update("INSERT INTO exam_attempt(exam_id,student_id,status,started_at,score) VALUES(?,21,'GRADED',?,?)",exam.id(),now,threshold);
            jdbc.update("INSERT INTO exam_attempt(exam_id,student_id,status,started_at,score) VALUES(?,22,'GRADED',?,?)",exam.id(),now,threshold-1);
            jdbc.update("INSERT INTO exam_attempt(exam_id,student_id,status,started_at,score) VALUES(?,23,'SUBMITTED',?,?)",exam.id(),now,total);
            var before=classAnalytics.get(51L,teacher,false).exams().stream().filter(e->e.examId().equals(exam.id())).findFirst().orElseThrow();
            assertEquals(2,before.gradedAttemptCount());
            assertEquals(0,before.passRate().compareTo(new BigDecimal("50")));
            jdbc.update("UPDATE exam_attempt SET status='GRADED' WHERE exam_id=? AND student_id=23",exam.id());
            var after=classAnalytics.get(51L,teacher,false).exams().stream().filter(e->e.examId().equals(exam.id())).findFirst().orElseThrow();
            assertEquals(0,after.passRate().compareTo(new BigDecimal("66.67")));
        }
        assertEquals(0,classAnalytics.get(51L,teacher,false).examPassRate().compareTo(new BigDecimal("66.67")));
    }

    @Test void assignmentsEnforceTeacherStudentDeadlineAndGradingRulesAndNotifyIdempotently() {
        var teacher=actor(11,UserRole.TEACHER); var deadline=LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
        var assignment=assignments.create(new SaveAssignmentRequest(51L,"Homework","Write a REST endpoint",deadline),teacher);
        assertEquals("DRAFT",assignment.status().name());
        assertForbidden(()->assignments.publish(assignment.id(),actor(12,UserRole.TEACHER)));
        assignments.publish(assignment.id(),teacher);
        Long eventId=jdbc.queryForObject("SELECT id FROM message_outbox WHERE dedupe_key=?",Long.class,"assignment-published:"+assignment.id());
        assertEquals(2,notifications.recoverMissingAssignmentNotifications(100));
        notifications.consumeAssignmentPublished(eventId,assignment.id(),51L,"Homework");
        notifications.consumeAssignmentPublished(eventId,assignment.id(),51L,"Homework");
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE source_id=?",Integer.class,assignment.id()));
        var submitted=assignments.submit(assignment.id(),new SubmitAssignmentRequest("My solution"),actor(21,UserRole.STUDENT));
        assertEquals("SUBMITTED",submitted.status().name());
        assertForbidden(()->assignments.submit(assignment.id(),new SubmitAssignmentRequest("wrong class"),actor(23,UserRole.STUDENT)));
        assignments.grade(submitted.id(),new GradeSubmissionRequest(new BigDecimal("88"),"Good"),teacher);
        assertEquals("GRADED",jdbc.queryForObject("SELECT status FROM assignment_submission WHERE id=?",String.class,submitted.id()));
        assertInvalid(()->assignments.submit(assignment.id(),new SubmitAssignmentRequest("change after grade"),actor(21,UserRole.STUDENT)));
        Long notificationId=jdbc.queryForObject("SELECT id FROM notification WHERE user_id=21",Long.class);
        notifications.markRead(notificationId,actor(21,UserRole.STUDENT)); notifications.markRead(notificationId,actor(21,UserRole.STUDENT));
        assertEquals(0L,notifications.unread(actor(21,UserRole.STUDENT)));
        BusinessException denied=assertThrows(BusinessException.class,()->notifications.markRead(notificationId,actor(22,UserRole.STUDENT)));
        assertEquals("NOTIFICATION_NOT_FOUND",denied.getCode().name());
    }

    @Test void assignmentDeadlineUsesUtcEvenWhenTheServerDefaultZoneIsNotUtc(){
        TimeZone original=TimeZone.getDefault();
        try{
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            var deadline=LocalDateTime.now(ZoneOffset.UTC).plusHours(4);
            var teacher=actor(11,UserRole.TEACHER);
            var assignment=assignments.create(new SaveAssignmentRequest(51L,"UTC deadline","Use server UTC",deadline),teacher);
            assignments.publish(assignment.id(),teacher);
            assertEquals("SUBMITTED",assignments.submit(assignment.id(),new SubmitAssignmentRequest("On time"),actor(21,UserRole.STUDENT)).status().name());
        }finally{TimeZone.setDefault(original);}
    }

    @Test void examSnapshotsHideAnswersAndAttemptSubmissionAndGradingAreIdempotent() throws Exception {
        var teacher=actor(11,UserRole.TEACHER); var student=actor(21,UserRole.STUDENT);
        var question=examinations.createQuestion(new SaveQuestionRequest(31L,QuestionType.SINGLE_CHOICE,"2 + 2?",
                "[{\"id\":\"A\",\"text\":\"4\"},{\"id\":\"B\",\"text\":\"5\"}]","\"A\"",QuestionDifficulty.EASY),teacher);
        examinations.createQuestion(new SaveQuestionRequest(31L,QuestionType.SHORT_ANSWER,"Explain idempotency",null,
                "\"Answers should not duplicate effects\"",QuestionDifficulty.MEDIUM),teacher);
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        var exam=examinations.createExam(new CreateExamRequest(51L,"Midterm",now.minusMinutes(1),now.plusHours(1),30),teacher);
        var generated=examinations.generate(exam.id(),new GenerateExamRequest(List.of(
                new QuestionRule(QuestionType.SINGLE_CHOICE,QuestionDifficulty.EASY,1,new BigDecimal("40")),
                new QuestionRule(QuestionType.SHORT_ANSWER,QuestionDifficulty.MEDIUM,1,new BigDecimal("60")))),teacher);
        assertEquals(2,generated.questionCount()); examinations.publish(exam.id(),teacher);
        examinations.updateQuestion(question.id(),new SaveQuestionRequest(31L,QuestionType.SINGLE_CHOICE,"Changed source",
                "[{\"id\":\"A\",\"text\":\"4\"}]","\"B\"",QuestionDifficulty.EASY),teacher);
        var start=examinations.start(exam.id(),student);
        assertEquals(2,start.questions().size());
        String studentPaper=json.writeValueAsString(start);
        assertFalse(studentPaper.contains("answerSnapshot"));assertFalse(studentPaper.contains("correctAnswerJson"));
        assertForbidden(()->examinations.start(exam.id(),actor(23,UserRole.STUDENT)));
        var first=start.questions().get(0); var second=start.questions().get(1);
        examinations.saveAnswers(exam.id(),new SaveExamAnswersRequest(Map.of(first.id(),json.readTree("\"A\""))),student);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM exam_answer",Integer.class));
        assertEquals("A",examinations.start(exam.id(),student).savedAnswers().get(first.id()).asText());
        assertForbidden(()->examinations.saveAnswers(exam.id(),new SaveExamAnswersRequest(Map.of(first.id(),json.createObjectNode())),actor(23,UserRole.STUDENT)));
        var submitted=examinations.submit(exam.id(),new SubmitExamRequest(Map.of(second.id(),json.readTree("\"A sensible response\""))),student);
        assertEquals(AttemptStatus.SUBMITTED,submitted.status());
        assertEquals(new BigDecimal("40.00"),submitted.score());
        var changedAnswer=json.readTree("\"B\"");
        assertInvalid(()->examinations.saveAnswers(exam.id(),new SaveExamAnswersRequest(Map.of(first.id(),changedAnswer)),student));
        var repeated=examinations.submit(exam.id(),new SubmitExamRequest(Map.of(first.id(),json.readTree("\"B\""))),student);
        assertEquals(submitted.score(),repeated.score()); assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM exam_answer",Integer.class));
        assertInvalid(()->examinations.result(exam.id(),student));
        Long shortAnswerId=jdbc.queryForObject("SELECT a.id FROM exam_answer a JOIN exam_question q ON q.id=a.exam_question_id WHERE q.type='SHORT_ANSWER'",Long.class);
        assertForbidden(()->examinations.gradeAnswer(shortAnswerId,new GradeExamAnswerRequest(new BigDecimal("50"),"good"),actor(12,UserRole.TEACHER)));
        var graded=examinations.gradeAnswer(shortAnswerId,new GradeExamAnswerRequest(new BigDecimal("55"),"good"),teacher);
        assertEquals(AttemptStatus.GRADED,graded.status());
        var result=examinations.result(exam.id(),student);
        assertEquals(new BigDecimal("95.00"),result.score());
        assertTrue(result.answers().stream().anyMatch(a->a.type()==QuestionType.SINGLE_CHOICE && "\"A\"".equals(a.correctAnswerJson())));
        assertEquals(1,examinations.results(exam.id(),teacher).size());
        assertForbidden(()->examinations.results(exam.id(),actor(12,UserRole.TEACHER)));
    }

    @Test void examRejectsInsufficientQuestionPoolAndCrossClassResults() {
        var teacher=actor(11,UserRole.TEACHER); LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        var exam=examinations.createExam(new CreateExamRequest(51L,"Exam",now.minusMinutes(1),now.plusHours(1),20),teacher);
        assertInvalid(()->examinations.generate(exam.id(),new GenerateExamRequest(List.of(new QuestionRule(QuestionType.TRUE_FALSE,QuestionDifficulty.HARD,2,new BigDecimal("10")))),teacher));
    }

    @Test void answerDraftEndpointSavesAndRestoresOnlyTheStudentOwns() throws Exception {
        var teacher=actor(11,UserRole.TEACHER);LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        examinations.createQuestion(new SaveQuestionRequest(31L,QuestionType.SINGLE_CHOICE,"Choose A","[{\"id\":\"A\"}]","\"A\"",QuestionDifficulty.EASY),teacher);
        var exam=examinations.createExam(new CreateExamRequest(51L,"Draft API",now.minusMinutes(1),now.plusHours(1),20),teacher);
        examinations.generate(exam.id(),new GenerateExamRequest(List.of(new QuestionRule(QuestionType.SINGLE_CHOICE,QuestionDifficulty.EASY,1,new BigDecimal("100")))),teacher);
        examinations.publish(exam.id(),teacher);var start=examinations.start(exam.id(),actor(21,UserRole.STUDENT));Long questionId=start.questions().get(0).id();
        String body=json.writeValueAsString(Map.of("answers",Map.of(questionId,"A")));
        mvc.perform(put("/api/exams/{id}/answers",exam.id()).header("Authorization","Bearer "+token(21,UserRole.STUDENT)).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedAnswerCount").value(1));
        mvc.perform(post("/api/exams/{id}/start",exam.id()).header("Authorization","Bearer "+token(21,UserRole.STUDENT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedAnswers['"+questionId+"']").value("A"))
                .andExpect(jsonPath("$.data.questions[0].answerSnapshot").doesNotExist());
        mvc.perform(put("/api/exams/{id}/answers",exam.id()).header("Authorization","Bearer "+token(11,UserRole.TEACHER)).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test void concurrentSubjectiveGradingSerializesAndPublishesOneFinalScore() throws Exception {
        var teacher=actor(11,UserRole.TEACHER);var student=actor(21,UserRole.STUDENT);LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        examinations.createQuestion(new SaveQuestionRequest(31L,QuestionType.SHORT_ANSWER,"Explain transaction isolation",null,"\"consistent view\"",QuestionDifficulty.MEDIUM),teacher);
        examinations.createQuestion(new SaveQuestionRequest(31L,QuestionType.SHORT_ANSWER,"Explain idempotency",null,"\"repeatable operation\"",QuestionDifficulty.MEDIUM),teacher);
        var exam=examinations.createExam(new CreateExamRequest(51L,"Concurrent grading",now.minusMinutes(1),now.plusHours(1),30),teacher);
        examinations.generate(exam.id(),new GenerateExamRequest(List.of(new QuestionRule(QuestionType.SHORT_ANSWER,QuestionDifficulty.MEDIUM,2,new BigDecimal("50")))),teacher);
        examinations.publish(exam.id(),teacher);var start=examinations.start(exam.id(),student);
        Map<Long,com.fasterxml.jackson.databind.JsonNode> responses=Map.of(start.questions().get(0).id(),json.readTree("\"answer one\""),start.questions().get(1).id(),json.readTree("\"answer two\""));
        examinations.submit(exam.id(),new SubmitExamRequest(responses),student);
        List<Long> answerIds=jdbc.queryForList("SELECT id FROM exam_answer WHERE attempt_id=(SELECT id FROM exam_attempt WHERE exam_id=? AND student_id=21) ORDER BY id",Long.class,exam.id());
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try{
            var a=pool.submit(()->{ready.countDown();go.await();return examinations.gradeAnswer(answerIds.get(0),new GradeExamAnswerRequest(new BigDecimal("40"),"good"),teacher);});
            var b=pool.submit(()->{ready.countDown();go.await();return examinations.gradeAnswer(answerIds.get(1),new GradeExamAnswerRequest(new BigDecimal("35"),"good"),teacher);});
            ready.await();go.countDown();a.get();b.get();
        }finally{pool.shutdownNow();}
        assertEquals("GRADED",jdbc.queryForObject("SELECT status FROM exam_attempt WHERE exam_id=? AND student_id=21",String.class,exam.id()));
        assertEquals(new BigDecimal("75.00"),jdbc.queryForObject("SELECT score FROM exam_attempt WHERE exam_id=? AND student_id=21",BigDecimal.class,exam.id()));
    }

    @Test void expiredAttemptsAreAutomaticallySubmittedOnceAndControllerRolesAreEnforced() throws Exception {
        var teacher=actor(11,UserRole.TEACHER);LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        examinations.createQuestion(new SaveQuestionRequest(31L,QuestionType.TRUE_FALSE,"The sky is blue?","[true,false]","true",QuestionDifficulty.EASY),teacher);
        var exam=examinations.createExam(new CreateExamRequest(51L,"Timed",now.minusMinutes(1),now.plusHours(1),1),teacher);
        examinations.generate(exam.id(),new GenerateExamRequest(List.of(new QuestionRule(QuestionType.TRUE_FALSE,QuestionDifficulty.EASY,1,new BigDecimal("100")))),teacher);
        examinations.publish(exam.id(),teacher);var start=examinations.start(exam.id(),actor(21,UserRole.STUDENT));
        examinations.saveAnswers(exam.id(),new SaveExamAnswersRequest(Map.of(start.questions().get(0).id(),json.readTree("true"))),actor(21,UserRole.STUDENT));
        jdbc.update("UPDATE exam_attempt SET started_at=TIMESTAMP '2000-01-01 00:00:00' WHERE exam_id=? AND student_id=21",exam.id());
        assertEquals(1,examinations.expireOverdueAttempts(10));assertEquals(0,examinations.expireOverdueAttempts(10));
        assertEquals("GRADED",jdbc.queryForObject("SELECT status FROM exam_attempt WHERE exam_id=? AND student_id=21",String.class,exam.id()));
        assertEquals(new BigDecimal("100.00"),jdbc.queryForObject("SELECT score FROM exam_attempt WHERE exam_id=? AND student_id=21",BigDecimal.class,exam.id()));
        mvc.perform(get("/api/teacher/classes/51/students").header("Authorization","Bearer "+token(12,UserRole.TEACHER)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/students/me/attendance").header("Authorization","Bearer "+token(21,UserRole.STUDENT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("SUCCESS"));
        mvc.perform(get("/api/teacher/questions").header("Authorization","Bearer "+token(21,UserRole.STUDENT)))
                .andExpect(status().isForbidden());
    }

    @Test void teacherDashboardAndAuditLogAreScopedAndPersistMutationMetadata() throws Exception {
        var own=teacherDashboard.dashboard(actor(11,UserRole.TEACHER));
        assertEquals(1,own.responsibleClassCount());assertEquals(51L,own.classProgress().get(0).classId());
        mvc.perform(get("/api/teacher/dashboard").header("Authorization","Bearer "+token(11,UserRole.TEACHER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.responsibleClassCount").value(1));
        var other=teacherDashboard.dashboard(actor(12,UserRole.TEACHER));
        assertEquals(0,other.pendingAssignmentCount());assertEquals(52L,other.classProgress().get(0).classId());
        mvc.perform(post("/api/teacher/assignments").header("Authorization","Bearer "+token(11,UserRole.TEACHER)).header("X-Request-Id","audit-request-42")
                        .contentType(APPLICATION_JSON).content("{\"classId\":51,\"title\":\"Audit task\",\"content\":\"No secrets in audit\",\"deadline\":\""+LocalDateTime.now(ZoneOffset.UTC).plusHours(1)+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.requestId").value("audit-request-42"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Request-Id","audit-request-42"));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM operation_audit_log WHERE actor_id=11 AND actor_role='TEACHER' AND http_method='POST' AND request_path='/api/teacher/assignments' AND outcome='SUCCESS'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM operation_audit_log WHERE request_id='audit-request-42' AND actor_id=11",Integer.class));
        mvc.perform(get("/api/admin/audit-logs").header("Authorization","Bearer "+token(10,UserRole.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/audit-logs").header("Authorization","Bearer "+token(11,UserRole.TEACHER)))
                .andExpect(status().isForbidden());
    }

    @Test void mvcMethodValidationAndMissingRequiredQueryParametersReturnClientErrors() throws Exception {
        mvc.perform(put("/api/admin/classes/999999")
                        .header("Authorization", "Bearer " + token(10, UserRole.ADMIN))
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(patch("/api/admin/classes/999999/status")
                        .header("Authorization", "Bearer " + token(10, UserRole.ADMIN))
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(get("/api/classes")
                        .header("Authorization", "Bearer " + token(21, UserRole.STUDENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private AuthenticatedUser actor(long id,UserRole role){return new AuthenticatedUser(id,"test-"+id,role);}
    private String token(long id,UserRole role){UserEntity user=new UserEntity();user.setId(id);user.setUsername("test-"+id);user.setRole(role);return jwt.issue(user);}
    private void assertForbidden(Runnable f){BusinessException e=assertThrows(BusinessException.class,f::run);assertEquals("FORBIDDEN",e.getCode().name());}
    private void assertInvalid(Runnable f){BusinessException e=assertThrows(BusinessException.class,f::run);assertEquals("INVALID_STATE",e.getCode().name());}
}
