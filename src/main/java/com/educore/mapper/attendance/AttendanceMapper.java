package com.educore.mapper.attendance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.attendance.dto.AttendanceItemRequest;
import com.educore.attendance.entity.AttendanceEntity;
import com.educore.attendance.enums.AttendanceStatus;
import com.educore.attendance.vo.AttendanceView;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AttendanceMapper extends BaseMapper<AttendanceEntity> {
    @Select("SELECT s.id AS schedule_id,s.class_id,s.teacher_id FROM class_schedule s WHERE s.id=#{id} AND s.status='SCHEDULED'")
    @ConstructorArgs({@Arg(column="schedule_id",javaType=Long.class),@Arg(column="class_id",javaType=Long.class),@Arg(column="teacher_id",javaType=Long.class)})
    ScheduleAccess scheduleAccess(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM class_student WHERE class_id=#{classId} AND student_id=#{studentId} AND status='ENROLLED'")
    int isEnrolled(@Param("classId") Long classId, @Param("studentId") Long studentId);

    @Insert({"<script>","INSERT INTO attendance(schedule_id,student_id,status,checked_at) VALUES", "<foreach collection='records' item='row' separator=','>(#{scheduleId},#{row.studentId},#{row.status},CURRENT_TIMESTAMP(3))</foreach>","ON DUPLICATE KEY UPDATE status=VALUES(status),checked_at=CURRENT_TIMESTAMP(3),updated_at=CURRENT_TIMESTAMP(3)","</script>"})
    int upsertBatch(@Param("scheduleId") Long scheduleId, @Param("records") List<AttendanceItemRequest> records);

    @Select("SELECT a.id,a.schedule_id,a.student_id,s.class_id,c.name AS class_name,u.real_name AS student_name,a.status,a.checked_at FROM attendance a JOIN class_schedule s ON s.id=a.schedule_id JOIN edu_class c ON c.id=s.class_id JOIN sys_user u ON u.id=a.student_id WHERE a.student_id=#{studentId} ORDER BY a.checked_at DESC,a.id DESC")
    @ConstructorArgs({@Arg(column="id",javaType=Long.class),@Arg(column="schedule_id",javaType=Long.class),@Arg(column="class_id",javaType=Long.class),@Arg(column="class_name",javaType=String.class),@Arg(column="student_id",javaType=Long.class),@Arg(column="student_name",javaType=String.class),@Arg(column="status",javaType=AttendanceStatus.class),@Arg(column="checked_at",javaType=java.time.LocalDateTime.class)})
    List<AttendanceView> studentAttendance(@Param("studentId") Long studentId);

    @Select("SELECT u.id AS student_id,u.real_name,a.status,a.checked_at FROM class_schedule s JOIN class_student cs ON cs.class_id=s.class_id AND cs.status='ENROLLED' JOIN sys_user u ON u.id=cs.student_id LEFT JOIN attendance a ON a.schedule_id=s.id AND a.student_id=cs.student_id WHERE s.id=#{scheduleId} ORDER BY u.id")
    @ConstructorArgs({@Arg(column="student_id",javaType=Long.class),@Arg(column="real_name",javaType=String.class),@Arg(column="status",javaType=AttendanceStatus.class),@Arg(column="checked_at",javaType=java.time.LocalDateTime.class)})
    List<com.educore.attendance.vo.AttendanceRosterView> scheduleRoster(@Param("scheduleId") Long scheduleId);

    record ScheduleAccess(Long scheduleId, Long classId, Long teacherId) { }
}
