package com.educore.enrollment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.enrollment.entity.ClassStudentEntity;
import com.educore.enrollment.entity.vo.StudentClassView;
import com.educore.enrollment.entity.vo.ClassMemberView;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ClassStudentMapper extends BaseMapper<ClassStudentEntity> {
    @Select("SELECT * FROM class_student WHERE class_id=#{classId} AND student_id=#{studentId} AND status='ENROLLED'")
    ClassStudentEntity selectEnrolled(@Param("classId") Long classId, @Param("studentId") Long studentId);

    List<StudentClassView> selectStudentClasses(@Param("studentId") Long studentId);

    long countActiveCourseEnrollment(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Select("SELECT u.id AS student_id,u.username,u.real_name AS real_name,cs.enrolled_at FROM class_student cs JOIN sys_user u ON u.id=cs.student_id WHERE cs.class_id=#{classId} AND cs.status='ENROLLED' ORDER BY cs.enrolled_at,cs.student_id")
    @ConstructorArgs({@Arg(column="student_id",javaType=Long.class),@Arg(column="username",javaType=String.class),@Arg(column="real_name",javaType=String.class),@Arg(column="enrolled_at",javaType=java.time.LocalDateTime.class)})
    List<ClassMemberView> selectClassStudents(@Param("classId") Long classId);
}
