package com.educore.examination.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.examination.entity.QuestionEntity;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface QuestionMapper extends BaseMapper<QuestionEntity> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM question WHERE id=#{id} FOR UPDATE")
    QuestionEntity lockQuestion(@org.apache.ibatis.annotations.Param("id") Long id);
}
