package com.educore.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.attendance.entity.dto.AttendanceItemRequest;
import com.educore.attendance.entity.AttendanceEntity;
import com.educore.attendance.entity.enums.AttendanceStatus;
import com.educore.attendance.entity.vo.AttendanceView;
import com.educore.schedule.entity.enums.ScheduleStatus;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AttendanceMapper extends BaseMapper<AttendanceEntity> {
    @Select("SELECT s.id AS schedule_id,s.class_id,s.teacher_id,s.status FROM class_schedule s WHERE s.id=#{id} AND s.status IN ('SCHEDULED','COMPLETED')")
    @ConstructorArgs({@Arg(column="schedule_id",javaType=Long.class),@Arg(column="class_id",javaType=Long.class),@Arg(column="teacher_id",javaType=Long.class),@Arg(column="status",javaType=ScheduleStatus.class)})
    ScheduleAccess scheduleAccess(@Param("id") Long id);

    @Select("SELECT s.id AS schedule_id,s.class_id,s.teacher_id,s.status FROM class_schedule s WHERE s.id=#{id} AND s.status IN ('SCHEDULED','COMPLETED') FOR UPDATE")
    @ConstructorArgs({@Arg(column="schedule_id",javaType=Long.class),@Arg(column="class_id",javaType=Long.class),@Arg(column="teacher_id",javaType=Long.class),@Arg(column="status",javaType=ScheduleStatus.class)})
    ScheduleAccess scheduleAccessForUpdate(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM class_student WHERE class_id=#{classId} AND student_id=#{studentId} AND status='ENROLLED'")
    int isEnrolled(@Param("classId") Long classId, @Param("studentId") Long studentId);

    int upsertBatch(@Param("scheduleId") Long scheduleId, @Param("records") List<AttendanceItemRequest> records);

    List<AttendanceView> studentAttendance(@Param("studentId") Long studentId);

    List<com.educore.attendance.entity.vo.AttendanceRosterView> scheduleRoster(@Param("scheduleId") Long scheduleId);

    @Select("SELECT COUNT(*) FROM class_student cs LEFT JOIN attendance a ON a.schedule_id=#{scheduleId} AND a.student_id=cs.student_id WHERE cs.class_id=#{classId} AND cs.status='ENROLLED' AND a.id IS NULL")
    long countMissingAttendance(@Param("scheduleId") Long scheduleId,@Param("classId") Long classId);

    record ScheduleAccess(Long scheduleId, Long classId, Long teacherId, ScheduleStatus status) { }
}
