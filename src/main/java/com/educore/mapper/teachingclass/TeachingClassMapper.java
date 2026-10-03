package com.educore.mapper.teachingclass;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.teachingclass.entity.TeachingClassEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
@Mapper public interface TeachingClassMapper extends BaseMapper<TeachingClassEntity> {
 @Select("SELECT * FROM edu_class WHERE id = #{id} FOR UPDATE") TeachingClassEntity selectByIdForUpdate(@Param("id") Long id);
 @Select("SELECT COUNT(*) FROM class_schedule WHERE class_id=#{classId} AND status='SCHEDULED'") long countScheduledLessons(@Param("classId") Long classId);
 @Select("SELECT COUNT(*) FROM class_schedule s JOIN classroom r ON r.id=s.classroom_id WHERE s.class_id=#{classId} AND s.status='SCHEDULED' AND r.capacity < #{capacity}") long countSchedulesWithSmallRoom(@Param("classId") Long classId,@Param("capacity") int capacity);
 @Select("SELECT COUNT(*) FROM class_schedule WHERE class_id=#{classId} AND status='SCHEDULED' AND (start_time < #{earliest} OR end_time > #{latest})") long countSchedulesOutsideDates(@Param("classId") Long classId,@Param("earliest") java.time.LocalDateTime earliest,@Param("latest") java.time.LocalDateTime latest);
 @org.apache.ibatis.annotations.Update("UPDATE edu_class SET reserved_count=reserved_count+1 WHERE id=#{classId} AND status='ENROLLING' AND reserved_count+enrolled_count<capacity") int reserveEnrollmentSeat(@Param("classId") Long classId);
 @org.apache.ibatis.annotations.Update("UPDATE edu_class SET reserved_count=reserved_count-1 WHERE id=#{classId} AND reserved_count>0") int releaseEnrollmentSeat(@Param("classId") Long classId);
 @org.apache.ibatis.annotations.Update("UPDATE edu_class SET reserved_count=reserved_count-1,enrolled_count=enrolled_count+1 WHERE id=#{classId} AND status IN ('ENROLLING','IN_PROGRESS') AND reserved_count>0") int confirmEnrollmentSeat(@Param("classId") Long classId);
}
