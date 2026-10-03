package com.educore.mapper.examination;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.examination.entity.ExamAttemptEntity;
import org.apache.ibatis.annotations.*;
@Mapper public interface ExamAttemptMapper extends BaseMapper<ExamAttemptEntity> {
    @Select("SELECT * FROM exam_attempt WHERE exam_id=#{examId} AND student_id=#{studentId} FOR UPDATE") ExamAttemptEntity lockByExamStudent(@Param("examId") Long examId,@Param("studentId") Long studentId);
    @Select("SELECT * FROM exam_attempt WHERE id=#{id} FOR UPDATE") ExamAttemptEntity lockById(@Param("id") Long id);
    @Update("UPDATE exam_attempt SET status=#{status},submitted_at=#{submittedAt},score=#{score},updated_at=CURRENT_TIMESTAMP(3) WHERE id=#{id}") int updateAttempt(ExamAttemptEntity attempt);
    @Select("SELECT a.id FROM exam_attempt a JOIN exam e ON e.id=a.exam_id WHERE a.status='IN_PROGRESS' AND LEAST(TIMESTAMPADD(MINUTE,e.duration_minutes,a.started_at),e.end_time)<=CURRENT_TIMESTAMP(3) ORDER BY a.id LIMIT #{limit}")
    java.util.List<Long> expiredAttemptIds(@Param("limit") int limit);
}
