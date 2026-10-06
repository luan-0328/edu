package com.educore.dashboard.mapper;

import com.educore.dashboard.entity.vo.ClassTeachingSummaryView;
import com.educore.dashboard.entity.vo.PopularCourseView;
import com.educore.dashboard.entity.vo.StudentClassProgressView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DashboardMapper {
    @Select("SELECT COUNT(*) FROM sys_user WHERE role='STUDENT'") long totalStudents();
    @Select("SELECT COUNT(*) FROM edu_class WHERE status='IN_PROGRESS'") long teachingClassCount();
    @Select("SELECT COUNT(*) FROM class_student cs JOIN edu_class ec ON ec.id=cs.class_id WHERE cs.status='ENROLLED' AND ec.status IN ('ENROLLING','IN_PROGRESS')") long currentEnrollmentCount();
    @Select("SELECT COUNT(*) FROM enrollment_order WHERE status='PAID'") long paidOrderCount();
    @Select("SELECT COALESCE(SUM(amount),0) FROM payment_record WHERE status='SUCCESS'") java.math.BigDecimal simulatedOrderAmount();
    @Select("SELECT COUNT(*) FROM class_student WHERE status='ENROLLED' AND enrolled_at>=#{monthStart} AND enrolled_at<#{monthEnd}") long newEnrollments(@Param("monthStart") LocalDateTime monthStart,@Param("monthEnd") LocalDateTime monthEnd);

    List<PopularCourseView> popularCourses();

    List<ClassTeachingSummaryView> classTeaching();

    List<StudentClassProgressView> studentClassProgress(@Param("studentId") Long studentId);
}
