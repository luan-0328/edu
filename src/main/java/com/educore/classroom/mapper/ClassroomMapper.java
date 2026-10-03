package com.educore.classroom.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.classroom.entity.ClassroomEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
@Mapper public interface ClassroomMapper extends BaseMapper<ClassroomEntity> {
 @Select("SELECT * FROM classroom WHERE id = #{id} FOR UPDATE") ClassroomEntity selectByIdForUpdate(@Param("id") Long id);
 @Select("SELECT COUNT(*) FROM class_schedule s JOIN edu_class c ON c.id=s.class_id WHERE s.classroom_id=#{id} AND s.status='SCHEDULED' AND s.end_time > CURRENT_TIMESTAMP(3) AND c.capacity > #{capacity}") long countOversizedScheduledClasses(@Param("id") Long id,@Param("capacity") int capacity);
}
