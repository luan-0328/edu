package com.educore.schedule;

import com.educore.common.BusinessException;
import com.educore.schedule.dto.SaveScheduleRequest;
import com.educore.schedule.service.ScheduleService;
import com.educore.security.JwtService;
import com.educore.course.dto.SaveCourseRequest;
import com.educore.course.enums.CourseStatus;
import com.educore.course.service.CourseService;
import com.educore.teachingclass.dto.SaveClassRequest;
import com.educore.teachingclass.enums.ClassStatus;
import com.educore.teachingclass.service.TeachingClassService;
import com.educore.user.entity.UserEntity;
import com.educore.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:educore-phase2;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.datasource.hikari.connection-init-sql=","spring.flyway.enabled=false","spring.sql.init.mode=always",
 "spring.sql.init.schema-locations=classpath:schema.sql","educore.jwt.secret=phase-two-integration-test-secret-32-bytes-minimum","educore.jwt.ttl-seconds=3600"
})
@AutoConfigureMockMvc
class ScheduleIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired ScheduleService schedules;@Autowired CourseService courses;@Autowired TeachingClassService classes;@Autowired JwtService jwt;@Autowired MockMvc mvc;
 private LocalDateTime start;
 @BeforeEach void setUp(){
  jdbc.update("DELETE FROM class_schedule");jdbc.update("DELETE FROM edu_class");jdbc.update("DELETE FROM classroom");jdbc.update("DELETE FROM course");jdbc.update("DELETE FROM sys_user");
  jdbc.update("INSERT INTO sys_user(id,username,password_hash,real_name,role,status) VALUES(11,'teacher-a','hash','Teacher A','TEACHER','ACTIVE'),(12,'teacher-b','hash','Teacher B','TEACHER','ACTIVE'),(13,'student-a','hash','Student A','STUDENT','ACTIVE'),(14,'admin-a','hash','Admin A','ADMIN','ACTIVE')");
  jdbc.update("INSERT INTO course(id,name,price,status) VALUES(21,'Course A',100,'PUBLISHED'),(22,'Course B',100,'PUBLISHED')");
  LocalDate today=LocalDate.now(ZoneOffset.UTC);
  jdbc.update("INSERT INTO edu_class(id,course_id,teacher_id,name,capacity,reserved_count,enrolled_count,start_date,end_date,status) VALUES(31,21,11,'Class A',20,0,0,?,?, 'ENROLLING'),(32,22,11,'Class B',20,0,0,?,?, 'ENROLLING'),(33,22,12,'Class C',20,0,0,?,?, 'ENROLLING')",today.minusDays(1),today.plusDays(10),today.minusDays(1),today.plusDays(10),today.minusDays(1),today.plusDays(10));
  jdbc.update("INSERT INTO classroom(id,name,capacity,status) VALUES(41,'Room A',30,'ACTIVE'),(42,'Room B',30,'ACTIVE'),(43,'Room C',30,'ACTIVE')");
  start=today.plusDays(2).atTime(9,0);
 }
 @Test void halfOpenIntervalsAllowBackToBackLessonsButRejectOverlap(){
  schedules.create(req(31,41,start,start.plusHours(1)));
  assertDoesNotThrow(()->schedules.create(req(32,42,start.plusHours(1),start.plusHours(2))));
  BusinessException ex=assertThrows(BusinessException.class,()->schedules.create(req(32,43,start.plusMinutes(30),start.plusHours(1).plusMinutes(30))));
  assertEquals("SCHEDULE_CONFLICT",ex.getCode().name());
 }
 @Test void roomConflictIsRejectedEvenWhenTeachersDiffer(){
  schedules.create(req(31,41,start,start.plusHours(1)));
  BusinessException ex=assertThrows(BusinessException.class,()->schedules.create(req(33,41,start.plusMinutes(10),start.plusHours(1))));
  assertEquals("SCHEDULE_CONFLICT",ex.getCode().name());
 }
 @Test void modifyingScheduleExcludesItselfAndChecksTheNewWindow(){
  var first=schedules.create(req(31,41,start,start.plusHours(1)));
  schedules.create(req(32,42,start.plusHours(2),start.plusHours(3)));
  BusinessException ex=assertThrows(BusinessException.class,()->schedules.update(first.id(),req(31,41,start.plusHours(2),start.plusHours(3))));
  assertEquals("SCHEDULE_CONFLICT",ex.getCode().name());
  assertDoesNotThrow(()->schedules.update(first.id(),req(31,41,start.plusHours(4),start.plusHours(5))));
 }
 @Test void concurrentBookingsForOneTeacherCannotCreateOverlappingLessons()throws Exception{
  var pool=Executors.newFixedThreadPool(2);CountDownLatch ready=new CountDownLatch(2);CountDownLatch fire=new CountDownLatch(1);
  try{
   var a=pool.submit(()->attempt(31,41,ready,fire));var b=pool.submit(()->attempt(32,42,ready,fire));ready.await();fire.countDown();
   int successes=(a.get()?1:0)+(b.get()?1:0);assertEquals(1,successes);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM class_schedule WHERE status='SCHEDULED'",Integer.class));
  }finally{pool.shutdownNow();}
 }
 @Test void teacherCanOnlyReadOwnClassesAndNonTeacherCannotReadTeacherEndpoint()throws Exception{
  mvc.perform(get("/api/teacher/classes").header("Authorization","Bearer "+token(11,UserRole.TEACHER))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2)).andExpect(jsonPath("$.data[0].teacherId").value(11));
  mvc.perform(get("/api/teacher/classes").header("Authorization","Bearer "+token(13,UserRole.STUDENT))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
  mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+token(11,UserRole.TEACHER))).andExpect(status().isForbidden());
 }
 @Test void classTeacherScheduleIsFilteredToAuthenticatedTeacher()throws Exception{
  schedules.create(req(31,41,start,start.plusHours(1)));schedules.create(req(33,43,start.plusHours(1),start.plusHours(2)));
  mvc.perform(get("/api/teacher/schedules").header("Authorization","Bearer "+token(11,UserRole.TEACHER))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].teacherId").value(11));
 }
 @Test void enrollmentOpensOnlyAfterCourseIsPublished(){
  var course=courses.create(new SaveCourseRequest("New Course","description",new java.math.BigDecimal("199.00")));
  var teachingClass=classes.create(new SaveClassRequest(course.id(),11L,"New Class",18,LocalDate.now(ZoneOffset.UTC).plusDays(2),LocalDate.now(ZoneOffset.UTC).plusDays(8)));
  BusinessException ex=assertThrows(BusinessException.class,()->classes.updateStatus(teachingClass.id(),ClassStatus.ENROLLING));
  assertEquals("INVALID_STATE",ex.getCode().name());
  courses.updateStatus(course.id(),CourseStatus.PUBLISHED);
  assertEquals(ClassStatus.ENROLLING,classes.updateStatus(teachingClass.id(),ClassStatus.ENROLLING).status());
 }
 @Test void capacityCannotBeReducedBelowReservedAndEnrolledSeats(){
  jdbc.update("UPDATE edu_class SET reserved_count=2,enrolled_count=3 WHERE id=31");
  SaveClassRequest smaller=new SaveClassRequest(21L,11L,"Class A",4,LocalDate.now(ZoneOffset.UTC).minusDays(1),LocalDate.now(ZoneOffset.UTC).plusDays(10));
  BusinessException ex=assertThrows(BusinessException.class,()->classes.update(31L,smaller));
  assertEquals("CAPACITY_EXCEEDED",ex.getCode().name());
 }
 @Test void cancellingAnUnstartedClassCancelsItsLessonsAndReleasesHistorySafely(){
  var course=courses.create(new SaveCourseRequest("Cancellation Course",null,new java.math.BigDecimal("99.00")));
  var teachingClass=classes.create(new SaveClassRequest(course.id(),11L,"Cancellation Class",12,LocalDate.now(ZoneOffset.UTC).plusDays(1),LocalDate.now(ZoneOffset.UTC).plusDays(9)));
  courses.updateStatus(course.id(),CourseStatus.PUBLISHED);classes.updateStatus(teachingClass.id(),ClassStatus.ENROLLING);
  LocalDateTime lesson=LocalDate.now(ZoneOffset.UTC).plusDays(2).atTime(9,0);
  var scheduled=schedules.create(req(teachingClass.id(),41,lesson,lesson.plusHours(1)));
  assertNotNull(scheduled.id());
  assertEquals(ClassStatus.CANCELLED,classes.updateStatus(teachingClass.id(),ClassStatus.CANCELLED).status());
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM class_schedule WHERE class_id=? AND status='CANCELLED'",Integer.class,teachingClass.id()));
  assertTrue(schedules.classSchedules(teachingClass.id()).isEmpty());
 }
 private boolean attempt(long classId,long roomId,CountDownLatch ready,CountDownLatch fire)throws Exception{
  ready.countDown();fire.await();try{schedules.create(req(classId,roomId,start,start.plusHours(1)));return true;}catch(BusinessException expected){assertEquals("SCHEDULE_CONFLICT",expected.getCode().name());return false;}
 }
 private SaveScheduleRequest req(long classId,long roomId,LocalDateTime from,LocalDateTime to){return new SaveScheduleRequest(classId,roomId,from,to);}
 private String token(long id,UserRole role){UserEntity user=new UserEntity();user.setId(id);user.setRole(role);return jwt.issue(user);}
}
