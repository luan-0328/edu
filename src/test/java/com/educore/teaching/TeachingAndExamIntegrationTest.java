package com.educore.teaching;

import com.educore.assignment.dto.*;
import com.educore.assignment.service.AssignmentService;
import com.educore.attendance.dto.*;
import com.educore.attendance.enums.AttendanceStatus;
import com.educore.attendance.service.AttendanceService;
import com.educore.common.BusinessException;
import com.educore.examination.dto.*;
import com.educore.examination.enums.*;
import com.educore.examination.service.ExaminationService;
import com.educore.notification.service.NotificationService;
import com.educore.security.AuthenticatedUser;
import com.educore.security.JwtService;
import com.educore.user.entity.UserEntity;
import com.educore.user.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @Autowired AssignmentService assignments;
    @Autowired NotificationService notifications;
    @Autowired ExaminationService examinations;
    @Autowired ObjectMapper json;
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    @BeforeEach void seed() {
        jdbc.update("DELETE FROM exam_answer"); jdbc.update("DELETE FROM exam_attempt"); jdbc.update("DELETE FROM exam_question");
        jdbc.update("DELETE FROM exam"); jdbc.update("DELETE FROM question"); jdbc.update("DELETE FROM notification");
        jdbc.update("DELETE FROM message_consume_log"); jdbc.update("DELETE FROM message_outbox");
        jdbc.update("DELETE FROM assignment_submission"); jdbc.update("DELETE FROM assignment"); jdbc.update("DELETE FROM attendance");
        jdbc.update("DELETE FROM class_student"); jdbc.update("DELETE FROM class_schedule"); jdbc.update("DELETE FROM edu_class");
        jdbc.update("DELETE FROM classroom"); jdbc.update("DELETE FROM course"); jdbc.update("DELETE FROM sys_user");
        jdbc.update("INSERT INTO sys_user(id,username,password_hash,real_name,role,status) VALUES(11,'teach-a','hash','Teacher A','TEACHER','ACTIVE'),(12,'teach-b','hash','Teacher B','TEACHER','ACTIVE'),(21,'student-a','hash','Student A','STUDENT','ACTIVE'),(22,'student-b','hash','Student B','STUDENT','ACTIVE'),(23,'student-outside','hash','Outside Student','STUDENT','ACTIVE')");
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

    @Test void assignmentsEnforceTeacherStudentDeadlineAndGradingRulesAndNotifyIdempotently() {
        var teacher=actor(11,UserRole.TEACHER); var deadline=LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
        var assignment=assignments.create(new SaveAssignmentRequest(51L,"Homework","Write a REST endpoint",deadline),teacher);
        assertEquals("DRAFT",assignment.status().name());
        assertForbidden(()->assignments.publish(assignment.id(),actor(12,UserRole.TEACHER)));
        assignments.publish(assignment.id(),teacher);
        Long eventId=jdbc.queryForObject("SELECT id FROM message_outbox WHERE dedupe_key=?",Long.class,"assignment-published:"+assignment.id());
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
        assertFalse(json.writeValueAsString(start).contains("answer"));
        assertForbidden(()->examinations.start(exam.id(),actor(23,UserRole.STUDENT)));
        var first=start.questions().get(0); var second=start.questions().get(1);
        var submitted=examinations.submit(exam.id(),new SubmitExamRequest(Map.of(first.id(),json.readTree("\"A\""),second.id(),json.readTree("\"A sensible response\""))),student);
        assertEquals(AttemptStatus.SUBMITTED,submitted.status());
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

    @Test void expiredAttemptsAreAutomaticallySubmittedOnceAndControllerRolesAreEnforced() throws Exception {
        var teacher=actor(11,UserRole.TEACHER);LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        examinations.createQuestion(new SaveQuestionRequest(31L,QuestionType.TRUE_FALSE,"The sky is blue?","[true,false]","true",QuestionDifficulty.EASY),teacher);
        var exam=examinations.createExam(new CreateExamRequest(51L,"Timed",now.minusMinutes(1),now.plusHours(1),1),teacher);
        examinations.generate(exam.id(),new GenerateExamRequest(List.of(new QuestionRule(QuestionType.TRUE_FALSE,QuestionDifficulty.EASY,1,new BigDecimal("100")))),teacher);
        examinations.publish(exam.id(),teacher);examinations.start(exam.id(),actor(21,UserRole.STUDENT));
        jdbc.update("UPDATE exam_attempt SET started_at=TIMESTAMP '2000-01-01 00:00:00' WHERE exam_id=? AND student_id=21",exam.id());
        assertEquals(1,examinations.expireOverdueAttempts(10));assertEquals(0,examinations.expireOverdueAttempts(10));
        assertEquals("GRADED",jdbc.queryForObject("SELECT status FROM exam_attempt WHERE exam_id=? AND student_id=21",String.class,exam.id()));
        assertEquals(new BigDecimal("0.00"),jdbc.queryForObject("SELECT score FROM exam_attempt WHERE exam_id=? AND student_id=21",BigDecimal.class,exam.id()));
        mvc.perform(get("/api/teacher/classes/51/students").header("Authorization","Bearer "+token(12,UserRole.TEACHER)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/students/me/attendance").header("Authorization","Bearer "+token(21,UserRole.STUDENT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("SUCCESS"));
        mvc.perform(get("/api/teacher/questions").header("Authorization","Bearer "+token(21,UserRole.STUDENT)))
                .andExpect(status().isForbidden());
    }

    private AuthenticatedUser actor(long id,UserRole role){return new AuthenticatedUser(id,"test-"+id,role);}
    private String token(long id,UserRole role){UserEntity user=new UserEntity();user.setId(id);user.setUsername("test-"+id);user.setRole(role);return jwt.issue(user);}
    private void assertForbidden(Runnable f){BusinessException e=assertThrows(BusinessException.class,f::run);assertEquals("FORBIDDEN",e.getCode().name());}
    private void assertInvalid(Runnable f){BusinessException e=assertThrows(BusinessException.class,f::run);assertEquals("INVALID_STATE",e.getCode().name());}
}
