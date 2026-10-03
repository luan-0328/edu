package com.educore.examination.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.examination.entity.ExamQuestionEntity;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper public interface ExamQuestionMapper extends BaseMapper<ExamQuestionEntity> {
    @Select("SELECT * FROM exam_question WHERE exam_id=#{examId} ORDER BY sort_order,id")
    List<ExamQuestionEntity> listByExam(@Param("examId") Long examId);
}
