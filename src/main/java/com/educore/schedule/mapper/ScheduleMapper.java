package com.educore.schedule.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.schedule.entity.ScheduleEntity;
import com.educore.schedule.entity.vo.ScheduleView;
import com.educore.schedule.entity.enums.ScheduleStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
import java.util.List;
@Mapper public interface ScheduleMapper extends BaseMapper<ScheduleEntity> {
 List<ScheduleView> selectClassScheduleViews(@Param("classId") Long classId);
 List<ScheduleView> selectTeacherScheduleViews(@Param("teacherId") Long teacherId);
 @Select("SELECT * FROM class_schedule WHERE id = #{id} FOR UPDATE") ScheduleEntity selectByIdForUpdate(@Param("id") Long id);
 @Select("SELECT COUNT(*) FROM class_schedule WHERE class_id=#{classId} AND status IN ('SCHEDULED','COMPLETED')") long countPlannedLessons(@Param("classId") Long classId);
 @Select("SELECT COUNT(*) FROM class_schedule WHERE class_id=#{classId} AND status='SCHEDULED'") long countIncompleteLessons(@Param("classId") Long classId);
 long countMissingAttendanceForClass(@Param("classId") Long classId);
 List<Long> findTeacherConflicts(@Param("resourceId") Long teacherId,@Param("startTime") LocalDateTime start,@Param("endTime") LocalDateTime end,@Param("excludeId") Long excludeId);
 List<Long> findClassroomConflicts(@Param("resourceId") Long classroomId,@Param("startTime") LocalDateTime start,@Param("endTime") LocalDateTime end,@Param("excludeId") Long excludeId);
 List<Long> findClassConflicts(@Param("resourceId") Long classId,@Param("startTime") LocalDateTime start,@Param("endTime") LocalDateTime end,@Param("excludeId") Long excludeId);
 @Update("UPDATE class_schedule SET status='CANCELLED', updated_at=CURRENT_TIMESTAMP(3) WHERE class_id=#{classId} AND status='SCHEDULED'") int cancelScheduledForClass(@Param("classId") Long classId);
}
