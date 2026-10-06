package com.educore.assignment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.assignment.entity.AssignmentEntity;
import com.educore.assignment.entity.AssignmentSubmissionEntity;
import com.educore.assignment.entity.vo.AssignmentView;
import com.educore.assignment.entity.vo.SubmissionProgressView;
import com.educore.dashboard.entity.vo.TeacherPendingAssignmentView;
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
    List<AssignmentView> listStudentAssignments(@Param("studentId") Long studentId);
    List<SubmissionProgressView> listProgress(@Param("classId") Long classId,@Param("assignmentId") Long assignmentId);
    List<TeacherPendingAssignmentView> pendingForTeacher(@Param("teacherId") Long teacherId,@Param("limit") int limit);
    @Select("SELECT COUNT(*) FROM assignment_submission s JOIN assignment a ON a.id=s.assignment_id WHERE a.teacher_id=#{teacherId} AND s.status='SUBMITTED'") long countPendingForTeacher(@Param("teacherId") Long teacherId);
}
