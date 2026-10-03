package com.educore.mapper.course;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.course.entity.CourseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
@Mapper public interface CourseMapper extends BaseMapper<CourseEntity> {
 @Select("SELECT * FROM course WHERE id = #{id} FOR UPDATE") CourseEntity selectByIdForUpdate(@Param("id") Long id);
 @Select("SELECT COUNT(*) FROM edu_class WHERE course_id = #{courseId} AND status = 'ENROLLING'") long countOpenClasses(@Param("courseId") Long courseId);
}
