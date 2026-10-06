package com.educore.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.educore.classroom.entity.ClassroomEntity;
import com.educore.classroom.service.ClassroomService;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.schedule.entity.dto.SaveScheduleRequest;
import com.educore.schedule.entity.ScheduleEntity;
import com.educore.schedule.entity.enums.ScheduleStatus;
import com.educore.schedule.mapper.ScheduleMapper;
import com.educore.attendance.mapper.AttendanceMapper;
import com.educore.schedule.entity.vo.ScheduleView;
import com.educore.security.AuthenticatedUser;
import com.educore.teachingclass.entity.TeachingClassEntity;
import com.educore.teachingclass.entity.enums.ClassStatus;
import com.educore.teachingclass.mapper.TeachingClassMapper;
import com.educore.user.entity.enums.UserRole;
import com.educore.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Service public class ScheduleService {
 private final ScheduleMapper schedules;private final TeachingClassMapper classes;private final ClassroomService classrooms;private final UserService users;private final AttendanceMapper attendance;
 public ScheduleService(ScheduleMapper schedules,TeachingClassMapper classes,ClassroomService classrooms,UserService users,AttendanceMapper attendance){this.schedules=schedules;this.classes=classes;this.classrooms=classrooms;this.users=users;this.attendance=attendance;}

 @Transactional public ScheduleView create(SaveScheduleRequest request){
  validateTime(request.startTime(),request.endTime());TeachingClassEntity snapshot=requireClass(request.classId());
  users.lockActiveTeacher(snapshot.getTeacherId());ClassroomEntity room=classrooms.lockActiveClassroom(request.classroomId());
  TeachingClassEntity teachingClass=lockClass(snapshot.getId());
  if(!teachingClass.getTeacherId().equals(snapshot.getTeacherId()))throw retry();
  validateResources(teachingClass,room,request.startTime(),request.endTime());checkConflicts(teachingClass.getTeacherId(),room.getId(),teachingClass.getId(),request.startTime(),request.endTime(),0L);
  ScheduleEntity e=new ScheduleEntity();e.setClassId(teachingClass.getId());e.setTeacherId(teachingClass.getTeacherId());e.setClassroomId(room.getId());e.setStartTime(request.startTime());e.setEndTime(request.endTime());e.setStatus(ScheduleStatus.SCHEDULED);schedules.insert(e);return ScheduleView.from(e);
 }
 @Transactional public ScheduleView update(Long id,SaveScheduleRequest request){
  validateTime(request.startTime(),request.endTime());ScheduleEntity snapshot=schedules.selectById(id);if(snapshot==null)throw new BusinessException(ApiErrorCode.SCHEDULE_NOT_FOUND,"课次不存在",HttpStatus.NOT_FOUND);
  TeachingClassEntity oldClass=requireClass(snapshot.getClassId());TeachingClassEntity newClass=requireClass(request.classId());
  TreeSet<Long> teacherIds=new TreeSet<>(List.of(oldClass.getTeacherId(),newClass.getTeacherId()));for(Long teacherId:teacherIds)users.lockTeacherRecord(teacherId);users.lockActiveTeacher(newClass.getTeacherId());
  TreeSet<Long> roomIds=new TreeSet<>(List.of(snapshot.getClassroomId(),request.classroomId()));List<ClassroomEntity> lockedRooms=new ArrayList<>();for(Long roomId:roomIds)lockedRooms.add(classrooms.lockClassroomRecord(roomId));
  TreeSet<Long> classIds=new TreeSet<>(List.of(snapshot.getClassId(),request.classId()));List<TeachingClassEntity> lockedClasses=new ArrayList<>();for(Long classId:classIds)lockedClasses.add(lockClass(classId));
  ScheduleEntity e=schedules.selectByIdForUpdate(id);if(e==null)throw new BusinessException(ApiErrorCode.SCHEDULE_NOT_FOUND,"课次不存在",HttpStatus.NOT_FOUND);
  if(e.getStatus()!=ScheduleStatus.SCHEDULED)throw invalid("仅待上课次可以修改");
  if(!e.getClassId().equals(snapshot.getClassId())||!e.getClassroomId().equals(snapshot.getClassroomId()))throw invalid("课次已被其他操作修改，请重新读取后再试");
  TeachingClassEntity teachingClass=lockedClasses.stream().filter(c->c.getId().equals(request.classId())).findFirst().orElseThrow();
  ClassroomEntity room=lockedRooms.stream().filter(c->c.getId().equals(request.classroomId())).findFirst().orElseThrow();
  if(room.getStatus()!=com.educore.classroom.entity.enums.ClassroomStatus.ACTIVE)throw new BusinessException(ApiErrorCode.RESOURCE_UNAVAILABLE,"教室不存在或未启用",HttpStatus.BAD_REQUEST);
  if(!teachingClass.getTeacherId().equals(newClass.getTeacherId()))throw retry();
  validateResources(teachingClass,room,request.startTime(),request.endTime());checkConflicts(teachingClass.getTeacherId(),room.getId(),teachingClass.getId(),request.startTime(),request.endTime(),id);
  e.setClassId(teachingClass.getId());e.setTeacherId(teachingClass.getTeacherId());e.setClassroomId(room.getId());e.setStartTime(request.startTime());e.setEndTime(request.endTime());schedules.updateById(e);return ScheduleView.from(e);
 }
 public List<ScheduleView> classSchedules(Long classId){requireClass(classId);return schedules.selectClassScheduleViews(classId);}
 public List<ScheduleView> teacherSchedules(AuthenticatedUser actor){
  if(actor.role()!=UserRole.TEACHER)throw new BusinessException(ApiErrorCode.FORBIDDEN,"仅教师可以查询教师课表",HttpStatus.FORBIDDEN);
  return schedules.selectTeacherScheduleViews(actor.id());
 }

 @Transactional public ScheduleView complete(Long id,AuthenticatedUser actor){
  if(actor==null||actor.role()!=UserRole.TEACHER)throw new BusinessException(ApiErrorCode.FORBIDDEN,"仅教师可以确认课次完成",HttpStatus.FORBIDDEN);
  ScheduleEntity schedule=schedules.selectByIdForUpdate(id);
  if(schedule==null||schedule.getStatus()==ScheduleStatus.CANCELLED)throw new BusinessException(ApiErrorCode.SCHEDULE_NOT_FOUND,"课次不存在",HttpStatus.NOT_FOUND);
  if(!schedule.getTeacherId().equals(actor.id()))throw new BusinessException(ApiErrorCode.FORBIDDEN,"只能确认自己所授课次",HttpStatus.FORBIDDEN);
  if(schedule.getStatus()==ScheduleStatus.COMPLETED)return ScheduleView.from(schedule);
  if(LocalDateTime.now(ZoneOffset.UTC).isBefore(schedule.getEndTime()))throw invalid("课次结束后才能确认完成");
  if(attendance.countMissingAttendance(schedule.getId(),schedule.getClassId())>0)throw invalid("请先为班级所有在读学生登记考勤");
  schedule.setStatus(ScheduleStatus.COMPLETED);schedules.updateById(schedule);return ScheduleView.from(schedule);
 }
 @Transactional(propagation=Propagation.MANDATORY) public void cancelScheduledForClass(Long classId){schedules.cancelScheduledForClass(classId);}
 private void checkConflicts(Long teacherId,Long roomId,Long classId,LocalDateTime start,LocalDateTime end,Long excludeId){
  if(!schedules.findTeacherConflicts(teacherId,start,end,excludeId).isEmpty()||!schedules.findClassroomConflicts(roomId,start,end,excludeId).isEmpty()||!schedules.findClassConflicts(classId,start,end,excludeId).isEmpty())
   throw new BusinessException(ApiErrorCode.SCHEDULE_CONFLICT,"教师、教室或班级在该时间段已有课次",HttpStatus.CONFLICT);
 }
 private void validateResources(TeachingClassEntity c,ClassroomEntity room,LocalDateTime start,LocalDateTime end){
  if(c.getStatus()!=ClassStatus.ENROLLING&&c.getStatus()!=ClassStatus.IN_PROGRESS)throw invalid("仅报名中或教学中的班级可以排课");
  if(room.getCapacity()<c.getCapacity())throw new BusinessException(ApiErrorCode.CAPACITY_EXCEEDED,"教室容量不能满足班级容量",HttpStatus.CONFLICT);
  LocalDateTime earliest=c.getStartDate().atStartOfDay();LocalDateTime latest=c.getEndDate().plusDays(1).atStartOfDay();
  if(start.isBefore(earliest)||end.isAfter(latest))throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"课次时间必须位于班级教学日期范围内");
 }
 private void validateTime(LocalDateTime start,LocalDateTime end){if(!start.isBefore(end))throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"课次开始时间必须早于结束时间");}
 private TeachingClassEntity requireClass(Long id){TeachingClassEntity e=classes.selectById(id);if(e==null)throw new BusinessException(ApiErrorCode.CLASS_NOT_FOUND,"班级不存在",HttpStatus.NOT_FOUND);return e;}
 private TeachingClassEntity lockClass(Long id){TeachingClassEntity e=classes.selectByIdForUpdate(id);if(e==null)throw new BusinessException(ApiErrorCode.CLASS_NOT_FOUND,"班级不存在",HttpStatus.NOT_FOUND);return e;}
 private BusinessException invalid(String m){return new BusinessException(ApiErrorCode.INVALID_STATE,m,HttpStatus.CONFLICT);}
 private BusinessException retry(){return new BusinessException(ApiErrorCode.INVALID_STATE,"班级教师已变更，请重新读取班级后再排课",HttpStatus.CONFLICT);}
}
