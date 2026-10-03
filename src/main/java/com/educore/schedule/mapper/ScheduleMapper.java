package com.educore.schedule.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.schedule.entity.ScheduleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
import java.util.List;
@Mapper public interface ScheduleMapper extends BaseMapper<ScheduleEntity> {
 @Select("SELECT * FROM class_schedule WHERE id = #{id} FOR UPDATE") ScheduleEntity selectByIdForUpdate(@Param("id") Long id);
 @Select("SELECT id FROM class_schedule WHERE status = 'SCHEDULED' AND teacher_id = #{resourceId} AND start_time < #{endTime} AND end_time > #{startTime} AND id <> #{excludeId} ORDER BY id FOR UPDATE")
 List<Long> findTeacherConflicts(@Param("resourceId") Long teacherId,@Param("startTime") LocalDateTime start,@Param("endTime") LocalDateTime end,@Param("excludeId") Long excludeId);
 @Select("SELECT id FROM class_schedule WHERE status = 'SCHEDULED' AND classroom_id = #{resourceId} AND start_time < #{endTime} AND end_time > #{startTime} AND id <> #{excludeId} ORDER BY id FOR UPDATE")
 List<Long> findClassroomConflicts(@Param("resourceId") Long classroomId,@Param("startTime") LocalDateTime start,@Param("endTime") LocalDateTime end,@Param("excludeId") Long excludeId);
 @Select("SELECT id FROM class_schedule WHERE status = 'SCHEDULED' AND class_id = #{resourceId} AND start_time < #{endTime} AND end_time > #{startTime} AND id <> #{excludeId} ORDER BY id FOR UPDATE")
 List<Long> findClassConflicts(@Param("resourceId") Long classId,@Param("startTime") LocalDateTime start,@Param("endTime") LocalDateTime end,@Param("excludeId") Long excludeId);
 @Update("UPDATE class_schedule SET status='CANCELLED', updated_at=CURRENT_TIMESTAMP(3) WHERE class_id=#{classId} AND status='SCHEDULED'") int cancelScheduledForClass(@Param("classId") Long classId);
}
