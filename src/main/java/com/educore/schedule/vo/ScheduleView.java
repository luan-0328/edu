package com.educore.schedule.vo;
import com.educore.schedule.entity.ScheduleEntity;
import com.educore.schedule.enums.ScheduleStatus;
import java.time.LocalDateTime;
public record ScheduleView(Long id,Long classId,Long teacherId,Long classroomId,LocalDateTime startTime,LocalDateTime endTime,ScheduleStatus status) {
 public static ScheduleView from(ScheduleEntity e){return new ScheduleView(e.getId(),e.getClassId(),e.getTeacherId(),e.getClassroomId(),e.getStartTime(),e.getEndTime(),e.getStatus());}
}
