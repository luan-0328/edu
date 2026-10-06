package com.educore.examination.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.examination.entity.ExamAnswerEntity;
import com.educore.examination.entity.vo.PendingExamAnswerView;
import com.educore.dashboard.entity.vo.TeacherPendingExamView;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
@Mapper public interface ExamAnswerMapper extends BaseMapper<ExamAnswerEntity> {
    @Select("SELECT * FROM exam_answer WHERE id=#{id} FOR UPDATE") ExamAnswerEntity lockById(@Param("id") Long id);
    @Select("SELECT * FROM exam_answer WHERE attempt_id=#{attemptId} AND exam_question_id=#{questionId}") ExamAnswerEntity byAttemptAndQuestion(@Param("attemptId") Long attemptId,@Param("questionId") Long questionId);
    @Select("SELECT * FROM exam_answer WHERE attempt_id=#{attemptId} ORDER BY id") java.util.List<ExamAnswerEntity> byAttempt(@Param("attemptId") Long attemptId);
    @Select("SELECT COUNT(*) FROM exam_answer a JOIN exam_attempt t ON t.id=a.attempt_id JOIN exam_question q ON q.id=a.exam_question_id JOIN exam e ON e.id=t.exam_id WHERE e.class_id=#{classId} AND t.status IN ('IN_PROGRESS','SUBMITTED')") long unfinishedAttemptsInClass(@Param("classId") Long classId);
    @Select("SELECT attempt_id FROM exam_answer WHERE id=#{answerId}") Long attemptIdByAnswer(@Param("answerId") Long answerId);
    GradeAccess gradeAccess(@Param("answerId") Long answerId);
    @Select("SELECT COUNT(*) FROM exam_answer a JOIN exam_question q ON q.id=a.exam_question_id WHERE a.attempt_id=#{attemptId} AND q.type='SHORT_ANSWER' AND a.score IS NULL") int ungradedShortAnswers(@Param("attemptId") Long attemptId);
    @Select("SELECT COALESCE(SUM(score),0) FROM exam_answer WHERE attempt_id=#{attemptId}") BigDecimal totalScore(@Param("attemptId") Long attemptId);
    java.util.List<PendingExamAnswerView> pendingSubjectiveAnswers(@Param("examId") Long examId,@Param("teacherId") Long teacherId);
    java.util.List<TeacherPendingExamView> pendingByExamForTeacher(@Param("teacherId") Long teacherId,@Param("limit") int limit);
    long countPendingForTeacher(@Param("teacherId") Long teacherId);
    @Update("UPDATE exam_answer SET score=#{score},feedback=#{feedback},graded_at=CURRENT_TIMESTAMP(3),updated_at=CURRENT_TIMESTAMP(3) WHERE id=#{id} AND score IS NULL") int grade(@Param("id") Long id,@Param("score") BigDecimal score,@Param("feedback") String feedback);
    record GradeAccess(Long answerId,Long attemptId,Long examQuestionId,String answerJson,String correctAnswerJson,
                       com.educore.examination.entity.enums.QuestionType type,BigDecimal maxScore,Long teacherId,
                       com.educore.examination.entity.enums.AttemptStatus attemptStatus) { }
}
