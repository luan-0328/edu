package com.educore.enrollment;

import com.educore.enrollment.service.EnrollmentService;
import com.educore.user.entity.enums.UserRole;
import com.educore.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:educore-enrollment;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.datasource.hikari.connection-init-sql=","spring.flyway.enabled=false","spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql","educore.jwt.secret=enrollment-integration-test-secret-32-bytes-minimum",
        "educore.messaging.enabled=false","educore.enrollment.expiration-enabled=false","educore.cache.enabled=false"
})
public class EnrollmentIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired EnrollmentService enrollment;

    public static LocalDateTime utcTimestamp(int precision){return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);}

    @BeforeEach void seed(){
        jdbc.update("DELETE FROM payment_record");jdbc.update("DELETE FROM enrollment_order");jdbc.update("DELETE FROM message_consume_log");jdbc.update("DELETE FROM message_outbox");
        jdbc.update("DELETE FROM class_student");jdbc.update("DELETE FROM edu_class");jdbc.update("DELETE FROM course");jdbc.update("DELETE FROM sys_user");
        jdbc.update("INSERT INTO sys_user(id,username,password_hash,real_name,role,status) VALUES(10,'admin','hash','Admin','ADMIN','ACTIVE'),(11,'teacher','hash','Teacher','TEACHER','ACTIVE'),(21,'student-a','hash','Student A','STUDENT','ACTIVE'),(22,'student-b','hash','Student B','STUDENT','ACTIVE')");
        jdbc.update("INSERT INTO course(id,name,price,status) VALUES(31,'Java',199.00,'PUBLISHED')");
        LocalDate today=LocalDate.now(ZoneOffset.UTC);
        jdbc.update("INSERT INTO edu_class(id,course_id,teacher_id,name,capacity,reserved_count,enrolled_count,start_date,end_date,status) VALUES(51,31,11,'Java A',2,0,0,?,?, 'ENROLLING')",today.plusDays(1),today.plusDays(30));
    }

    @Test void expiredOrderIsLockedReleasedOnceAndReplacedWithoutChangingSeatCount(){
        var first=enrollment.createOrder(21L,51L);
        jdbc.update("UPDATE enrollment_order SET expire_at=? WHERE id=?",LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1),first.id());
        var replacement=enrollment.createOrder(21L,51L);
        assertNotEquals(first.id(),replacement.id());
        assertEquals("EXPIRED",jdbc.queryForObject("SELECT status FROM enrollment_order WHERE id=?",String.class,first.id()));
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM enrollment_order WHERE id=?",String.class,replacement.id()));
        assertEquals(1,jdbc.queryForObject("SELECT reserved_count FROM edu_class WHERE id=51",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM message_outbox",Integer.class));
    }

    @Test void repeatedPaymentMovesTheReservationIntoMembershipOnlyOnce(){
        var order=enrollment.createOrder(21L,51L);
        var paid=enrollment.pay(21L,order.id());
        var repeated=enrollment.pay(21L,order.id());
        assertEquals("PAID",paid.order().status().name());
        assertEquals("PAID",repeated.order().status().name());
        assertEquals(0,jdbc.queryForObject("SELECT reserved_count FROM edu_class WHERE id=51",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT enrolled_count FROM edu_class WHERE id=51",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM class_student WHERE class_id=51 AND student_id=21",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM payment_record WHERE order_id=?",Integer.class,order.id()));
    }

    @Test void concurrentSameStudentRequestsReuseOnePendingOrder() throws Exception {
        var pool=Executors.newFixedThreadPool(2);CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
        try{
            var a=pool.submit(()->{ready.countDown();go.await();return enrollment.createOrder(21L,51L);});
            var b=pool.submit(()->{ready.countDown();go.await();return enrollment.createOrder(21L,51L);});
            ready.await();go.countDown();var one=a.get();var two=b.get();
            assertEquals(one.id(),two.id());
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM enrollment_order WHERE student_id=21 AND class_id=51 AND status='PENDING'",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT reserved_count FROM edu_class WHERE id=51",Integer.class));
        }finally{pool.shutdownNow();}
    }
}
