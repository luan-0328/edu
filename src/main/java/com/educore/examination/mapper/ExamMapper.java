package com.educore.examination.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.examination.entity.ExamEntity;
import com.educore.examination.entity.vo.ExamResultSummaryView;
import com.educore.examination.entity.vo.ExamSummaryView;
import com.educore.dashboard.entity.vo.StudentExamResultView;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper public interface ExamMapper extends BaseMapper<ExamEntity> {
    @Select("SELECT * FROM exam WHERE id=#{id} FOR UPDATE") ExamEntity lockExam(@Param("id") Long id);
    @Select("SELECT c.teacher_id,c.course_id FROM edu_class c WHERE c.id=#{id}") ClassAccess classAccess(@Param("id") Long id);
    @Select("SELECT COUNT(*) FROM class_student WHERE class_id=#{classId} AND student_id=#{studentId} AND status='ENROLLED'") int isMember(@Param("classId") Long classId,@Param("studentId") Long studentId);
    List<ExamSummaryView> listStudentExams(@Param("studentId") Long studentId);
    @Select("SELECT a.id AS attempt_id,a.student_id,u.real_name AS student_name,a.status,a.score,a.submitted_at FROM exam_attempt a JOIN sys_user u ON u.id=a.student_id WHERE a.exam_id=#{examId} ORDER BY a.student_id")
    @ConstructorArgs({@Arg(column="attempt_id",javaType=Long.class),@Arg(column="student_id",javaType=Long.class),@Arg(column="student_name",javaType=String.class),@Arg(column="status",javaType=com.educore.examination.entity.enums.AttemptStatus.class),@Arg(column="score",javaType=java.math.BigDecimal.class),@Arg(column="submitted_at",javaType=java.time.LocalDateTime.class)})
    List<ExamResultSummaryView> listResults(@Param("examId") Long examId);
    List<StudentExamResultView> recentStudentResults(@Param("studentId") Long studentId);
    record ClassAccess(Long teacherId,Long courseId) { }
}
