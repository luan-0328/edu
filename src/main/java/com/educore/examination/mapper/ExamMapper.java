package com.educore.examination.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.examination.entity.ExamEntity;
import com.educore.examination.vo.ExamResultSummaryView;
import com.educore.examination.vo.ExamSummaryView;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper public interface ExamMapper extends BaseMapper<ExamEntity> {
    @Select("SELECT * FROM exam WHERE id=#{id} FOR UPDATE") ExamEntity lockExam(@Param("id") Long id);
    @Select("SELECT c.teacher_id,c.course_id FROM edu_class c WHERE c.id=#{id}") ClassAccess classAccess(@Param("id") Long id);
    @Select("SELECT COUNT(*) FROM class_student WHERE class_id=#{classId} AND student_id=#{studentId} AND status='ENROLLED'") int isMember(@Param("classId") Long classId,@Param("studentId") Long studentId);
    @Select("SELECT e.id,e.class_id,c.name AS class_name,e.title,e.start_time,e.end_time,e.duration_minutes,e.status FROM exam e JOIN edu_class c ON c.id=e.class_id JOIN class_student cs ON cs.class_id=e.class_id AND cs.student_id=#{studentId} AND cs.status='ENROLLED' WHERE e.status IN ('PUBLISHED','CLOSED') ORDER BY e.start_time,e.id")
    @ConstructorArgs({@Arg(column="id",javaType=Long.class),@Arg(column="class_id",javaType=Long.class),@Arg(column="class_name",javaType=String.class),@Arg(column="title",javaType=String.class),@Arg(column="start_time",javaType=java.time.LocalDateTime.class),@Arg(column="end_time",javaType=java.time.LocalDateTime.class),@Arg(column="duration_minutes",javaType=Integer.class),@Arg(column="status",javaType=com.educore.examination.enums.ExamStatus.class)})
    List<ExamSummaryView> listStudentExams(@Param("studentId") Long studentId);
    @Select("SELECT a.id AS attempt_id,a.student_id,u.real_name AS student_name,a.status,a.score,a.submitted_at FROM exam_attempt a JOIN sys_user u ON u.id=a.student_id WHERE a.exam_id=#{examId} ORDER BY a.student_id")
    @ConstructorArgs({@Arg(column="attempt_id",javaType=Long.class),@Arg(column="student_id",javaType=Long.class),@Arg(column="student_name",javaType=String.class),@Arg(column="status",javaType=com.educore.examination.enums.AttemptStatus.class),@Arg(column="score",javaType=java.math.BigDecimal.class),@Arg(column="submitted_at",javaType=java.time.LocalDateTime.class)})
    List<ExamResultSummaryView> listResults(@Param("examId") Long examId);
    record ClassAccess(Long teacherId,Long courseId) { }
}
