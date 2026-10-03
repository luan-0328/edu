package com.educore.mapper.assignment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.assignment.entity.AssignmentEntity;
import com.educore.assignment.entity.AssignmentSubmissionEntity;
import com.educore.assignment.vo.AssignmentView;
import com.educore.assignment.vo.SubmissionProgressView;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AssignmentMapper extends BaseMapper<AssignmentEntity> {
    @Select("SELECT * FROM assignment WHERE id=#{id} FOR UPDATE") AssignmentEntity lockAssignment(@Param("id") Long id);
    @Select("SELECT COUNT(*) FROM edu_class WHERE id=#{classId} AND teacher_id=#{teacherId}") int ownsClass(@Param("classId") Long classId,@Param("teacherId") Long teacherId);
    @Select("SELECT COUNT(*) FROM class_student WHERE class_id=#{classId} AND student_id=#{studentId} AND status='ENROLLED'") int isMember(@Param("classId") Long classId,@Param("studentId") Long studentId);
    @Select("SELECT * FROM assignment_submission WHERE assignment_id=#{assignmentId} AND student_id=#{studentId} FOR UPDATE") AssignmentSubmissionEntity lockSubmission(@Param("assignmentId") Long assignmentId,@Param("studentId") Long studentId);
    @Select("SELECT * FROM assignment_submission WHERE id=#{id} FOR UPDATE") AssignmentSubmissionEntity lockSubmissionById(@Param("id") Long id);
    @Insert("INSERT INTO assignment_submission(assignment_id,student_id,content,status,submitted_at) VALUES(#{assignmentId},#{studentId},#{content},'SUBMITTED',CURRENT_TIMESTAMP(3))")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertSubmission(AssignmentSubmissionEntity submission);
    @Update("UPDATE assignment_submission SET content=#{content},status=#{status},score=#{score},feedback=#{feedback},submitted_at=#{submittedAt},graded_at=#{gradedAt},updated_at=CURRENT_TIMESTAMP(3) WHERE id=#{id}")
    int updateSubmission(AssignmentSubmissionEntity submission);
    @Select("SELECT a.id,a.class_id,c.name AS class_name,a.title,a.content,a.deadline,a.status,s.id AS submission_id,s.content AS submission_content,s.status AS submission_status,s.score,s.feedback,s.submitted_at FROM assignment a JOIN edu_class c ON c.id=a.class_id JOIN class_student cs ON cs.class_id=a.class_id AND cs.student_id=#{studentId} AND cs.status='ENROLLED' LEFT JOIN assignment_submission s ON s.assignment_id=a.id AND s.student_id=#{studentId} WHERE a.status='PUBLISHED' ORDER BY a.deadline,a.id")
    @ConstructorArgs({@Arg(column="id",javaType=Long.class),@Arg(column="class_id",javaType=Long.class),@Arg(column="class_name",javaType=String.class),@Arg(column="title",javaType=String.class),@Arg(column="content",javaType=String.class),@Arg(column="deadline",javaType=java.time.LocalDateTime.class),@Arg(column="status",javaType=com.educore.assignment.enums.AssignmentStatus.class),@Arg(column="submission_id",javaType=Long.class),@Arg(column="submission_content",javaType=String.class),@Arg(column="submission_status",javaType=String.class),@Arg(column="score",javaType=java.math.BigDecimal.class),@Arg(column="feedback",javaType=String.class),@Arg(column="submitted_at",javaType=java.time.LocalDateTime.class)})
    List<AssignmentView> listStudentAssignments(@Param("studentId") Long studentId);
    @Select("SELECT u.id AS student_id,u.real_name AS student_name,s.status,s.content,s.score,s.feedback,s.submitted_at,s.graded_at FROM class_student cs JOIN sys_user u ON u.id=cs.student_id LEFT JOIN assignment_submission s ON s.assignment_id=#{assignmentId} AND s.student_id=cs.student_id WHERE cs.class_id=#{classId} AND cs.status='ENROLLED' ORDER BY u.id")
    @ConstructorArgs({@Arg(column="student_id",javaType=Long.class),@Arg(column="student_name",javaType=String.class),@Arg(column="status",javaType=String.class),@Arg(column="content",javaType=String.class),@Arg(column="score",javaType=java.math.BigDecimal.class),@Arg(column="feedback",javaType=String.class),@Arg(column="submitted_at",javaType=java.time.LocalDateTime.class),@Arg(column="graded_at",javaType=java.time.LocalDateTime.class)})
    List<SubmissionProgressView> listProgress(@Param("classId") Long classId,@Param("assignmentId") Long assignmentId);
}
