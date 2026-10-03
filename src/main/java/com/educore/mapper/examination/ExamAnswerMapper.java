package com.educore.mapper.examination;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.examination.entity.ExamAnswerEntity;
import com.educore.examination.vo.PendingExamAnswerView;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
@Mapper public interface ExamAnswerMapper extends BaseMapper<ExamAnswerEntity> {
    @Select("SELECT * FROM exam_answer WHERE id=#{id} FOR UPDATE") ExamAnswerEntity lockById(@Param("id") Long id);
    @Select("SELECT a.id AS answer_id,a.attempt_id,a.exam_question_id,a.answer_json,q.answer_snapshot,q.type,q.score,e.teacher_id,t.status FROM exam_answer a JOIN exam_attempt t ON t.id=a.attempt_id JOIN exam_question q ON q.id=a.exam_question_id JOIN exam e ON e.id=t.exam_id WHERE a.id=#{answerId}")
    @ConstructorArgs({@Arg(column="answer_id",javaType=Long.class),@Arg(column="attempt_id",javaType=Long.class),@Arg(column="exam_question_id",javaType=Long.class),@Arg(column="answer_json",javaType=String.class),@Arg(column="answer_snapshot",javaType=String.class),@Arg(column="type",javaType=com.educore.examination.enums.QuestionType.class),@Arg(column="score",javaType=BigDecimal.class),@Arg(column="teacher_id",javaType=Long.class),@Arg(column="status",javaType=com.educore.examination.enums.AttemptStatus.class)})
    GradeAccess gradeAccess(@Param("answerId") Long answerId);
    @Select("SELECT COUNT(*) FROM exam_answer a JOIN exam_question q ON q.id=a.exam_question_id WHERE a.attempt_id=#{attemptId} AND q.type='SHORT_ANSWER' AND a.score IS NULL") int ungradedShortAnswers(@Param("attemptId") Long attemptId);
    @Select("SELECT COALESCE(SUM(score),0) FROM exam_answer WHERE attempt_id=#{attemptId}") BigDecimal totalScore(@Param("attemptId") Long attemptId);
    @Select("SELECT a.id AS answer_id,a.attempt_id,t.student_id,u.real_name AS student_name,q.id AS exam_question_id,q.type,q.content_snapshot AS content,a.answer_json,q.score AS max_score FROM exam_answer a JOIN exam_attempt t ON t.id=a.attempt_id JOIN sys_user u ON u.id=t.student_id JOIN exam_question q ON q.id=a.exam_question_id JOIN exam e ON e.id=t.exam_id WHERE e.id=#{examId} AND e.teacher_id=#{teacherId} AND t.status='SUBMITTED' AND q.type='SHORT_ANSWER' AND a.score IS NULL ORDER BY t.student_id,q.sort_order")
    @ConstructorArgs({@Arg(column="answer_id",javaType=Long.class),@Arg(column="attempt_id",javaType=Long.class),@Arg(column="student_id",javaType=Long.class),@Arg(column="student_name",javaType=String.class),@Arg(column="exam_question_id",javaType=Long.class),@Arg(column="type",javaType=com.educore.examination.enums.QuestionType.class),@Arg(column="content",javaType=String.class),@Arg(column="answer_json",javaType=String.class),@Arg(column="max_score",javaType=BigDecimal.class)})
    java.util.List<PendingExamAnswerView> pendingSubjectiveAnswers(@Param("examId") Long examId,@Param("teacherId") Long teacherId);
    @Update("UPDATE exam_answer SET score=#{score},feedback=#{feedback},graded_at=CURRENT_TIMESTAMP(3),updated_at=CURRENT_TIMESTAMP(3) WHERE id=#{id}") int grade(@Param("id") Long id,@Param("score") BigDecimal score,@Param("feedback") String feedback);
    record GradeAccess(Long answerId,Long attemptId,Long examQuestionId,String answerJson,String correctAnswerJson,
                       com.educore.examination.enums.QuestionType type,BigDecimal maxScore,Long teacherId,
                       com.educore.examination.enums.AttemptStatus attemptStatus) { }
}
