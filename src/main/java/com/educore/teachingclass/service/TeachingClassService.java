package com.educore.teachingclass.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.course.entity.CourseEntity;
import com.educore.course.entity.enums.CourseStatus;
import com.educore.course.service.CourseService;
import com.educore.security.AuthenticatedUser;
import com.educore.schedule.service.ScheduleService;
import com.educore.teachingclass.entity.dto.SaveClassRequest;
import com.educore.teachingclass.entity.dto.ClassStatusRequest;
import com.educore.teachingclass.entity.TeachingClassEntity;
import com.educore.teachingclass.entity.enums.ClassStatus;
import com.educore.teachingclass.mapper.TeachingClassMapper;
import com.educore.teachingclass.entity.vo.TeachingClassView;
import com.educore.user.entity.UserEntity;
import com.educore.user.entity.enums.UserRole;
import com.educore.user.service.UserService;
import com.educore.enrollment.mapper.ClassStudentMapper;
import com.educore.enrollment.entity.vo.ClassMemberView;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educore.common.PageView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TreeSet;
import java.util.Map;
import java.util.stream.Collectors;
import com.educore.course.mapper.CourseMapper;
import com.educore.user.mapper.UserMapper;

@Service public class TeachingClassService {
 private final TeachingClassMapper mapper;private final CourseService courses;private final UserService users;private final ScheduleService schedules;private final ClassStudentMapper memberships;private final CourseMapper courseMapper;private final UserMapper userMapper;
 public TeachingClassService(TeachingClassMapper mapper,CourseService courses,UserService users,ScheduleService schedules,ClassStudentMapper memberships,CourseMapper courseMapper,UserMapper userMapper){this.mapper=mapper;this.courses=courses;this.users=users;this.schedules=schedules;this.memberships=memberships;this.courseMapper=courseMapper;this.userMapper=userMapper;}
 @Transactional public TeachingClassView create(SaveClassRequest request){
  validateDates(request.startDate(),request.endDate());courses.lockCourse(request.courseId());users.lockActiveTeacher(request.teacherId());
  TeachingClassEntity e=new TeachingClassEntity();apply(e,request);e.setReservedCount(0);e.setEnrolledCount(0);e.setStatus(ClassStatus.DRAFT);mapper.insert(e);return view(e);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public TeachingClassView update(Long id,SaveClassRequest request){
  TeachingClassEntity snapshot=requireClass(id);if(snapshot.getStatus()!=ClassStatus.DRAFT&&snapshot.getStatus()!=ClassStatus.ENROLLING)throw invalid("仅草稿或报名中的班级可以修改");
  validateDates(request.startDate(),request.endDate());
  TreeSet<Long> courseIds=new TreeSet<>(List.of(snapshot.getCourseId(),request.courseId()));for(Long courseId:courseIds)courses.lockCourse(courseId);
  TreeSet<Long> teacherIds=new TreeSet<>(List.of(snapshot.getTeacherId(),request.teacherId()));for(Long teacherId:teacherIds)users.lockTeacherRecord(teacherId);users.lockActiveTeacher(request.teacherId());
  TeachingClassEntity e=mapper.selectByIdForUpdate(id);if(e==null)throw notFound();
  if(e.getStatus()!=snapshot.getStatus()||!e.getCourseId().equals(snapshot.getCourseId())||!e.getTeacherId().equals(snapshot.getTeacherId()))throw invalid("班级已被其他操作修改，请重新读取后再试");
  if(request.capacity()<e.getReservedCount()+e.getEnrolledCount())throw new BusinessException(ApiErrorCode.CAPACITY_EXCEEDED,"班级容量不能小于已占用名额",HttpStatus.CONFLICT);
  if((!e.getCourseId().equals(request.courseId())||!e.getTeacherId().equals(request.teacherId()))&&mapper.countTeachingContent(id)>0)throw invalid("班级已有作业或考试，不能更换课程或教师");
  if(e.getStatus()==ClassStatus.ENROLLING&&(!e.getCourseId().equals(request.courseId())||!e.getTeacherId().equals(request.teacherId()))&&(e.getReservedCount()>0||e.getEnrolledCount()>0||mapper.countScheduledLessons(id)>0))throw invalid("班级已有有效名额或课次，不能更换课程或教师");
  if(mapper.countSchedulesWithSmallRoom(id,request.capacity())>0)throw new BusinessException(ApiErrorCode.CAPACITY_EXCEEDED,"班级容量不能大于已有课次使用教室的容量",HttpStatus.CONFLICT);
  if(mapper.countSchedulesOutsideDates(id,request.startDate().atStartOfDay(),request.endDate().plusDays(1).atStartOfDay())>0)throw invalid("新教学日期范围与已排课次不匹配");
  apply(e,request);mapper.updateById(e);return view(e);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public TeachingClassView updateStatus(Long id,ClassStatus target){
  TeachingClassEntity snapshot=requireClass(id);
  courses.lockCourse(snapshot.getCourseId());users.lockTeacherRecord(snapshot.getTeacherId());
  TeachingClassEntity e=mapper.selectByIdForUpdate(id);if(e==null)throw notFound();
  if(e.getStatus()!=snapshot.getStatus()||!e.getCourseId().equals(snapshot.getCourseId())||!e.getTeacherId().equals(snapshot.getTeacherId()))throw invalid("班级已被其他操作修改，请重新读取后再试");
  ClassStatus from=e.getStatus();if(from==target)return view(e);
  LocalDate today=LocalDate.now(ZoneOffset.UTC);
  boolean valid=switch(target){
   case ENROLLING->from==ClassStatus.DRAFT;
   case IN_PROGRESS->from==ClassStatus.ENROLLING&& !today.isBefore(e.getStartDate());
   case FINISHED->from==ClassStatus.IN_PROGRESS&&today.isAfter(e.getEndDate())&&mapper.countPlannedLessons(id)>0&&mapper.countScheduledLessons(id)==0&&mapper.countMissingAttendance(id)==0&&mapper.countPublishedAssignments(id)==0&&mapper.countUngradedAssignmentSubmissions(id)==0&&mapper.countPublishedExams(id)==0&&mapper.countUnfinishedExamAttempts(id)==0;
   case CANCELLED->(from==ClassStatus.DRAFT||from==ClassStatus.ENROLLING)&&today.isBefore(e.getStartDate())&&e.getReservedCount()==0&&e.getEnrolledCount()==0;
   case DRAFT->false;
  };
  if(!valid)throw invalid("不允许的班级状态变更，或仍有未完成课次、考勤、作业、考试及待批成绩");
  if(target==ClassStatus.ENROLLING||target==ClassStatus.IN_PROGRESS)users.lockActiveTeacher(e.getTeacherId());
  if(target==ClassStatus.ENROLLING){CourseEntity course=courses.lockCourse(e.getCourseId());if(course.getStatus()!=CourseStatus.PUBLISHED)throw invalid("课程上架后才能开放班级报名");}
  e.setStatus(target);mapper.updateById(e);if(target==ClassStatus.CANCELLED)schedules.cancelScheduledForClass(id);return view(e);
 }
 public TeachingClassView detail(Long id){return view(requireClass(id));}
 public PageView<TeachingClassView> listAdmin(long page,long size){Page<TeachingClassEntity> p=mapper.selectPage(new Page<>(page,size),new LambdaQueryWrapper<TeachingClassEntity>().orderByAsc(TeachingClassEntity::getId));return PageView.from(p,views(p.getRecords()));}
 public List<TeachingClassView> openByCourse(Long courseId){return views(mapper.selectList(new LambdaQueryWrapper<TeachingClassEntity>().eq(TeachingClassEntity::getCourseId,courseId).eq(TeachingClassEntity::getStatus,ClassStatus.ENROLLING).orderByAsc(TeachingClassEntity::getStartDate)));}
 public List<ClassMemberView> students(Long classId,AuthenticatedUser actor){if(actor==null||actor.role()!=UserRole.TEACHER)throw new BusinessException(ApiErrorCode.FORBIDDEN,"仅教师可以查询班级学生",HttpStatus.FORBIDDEN);if(mapper.selectCount(new LambdaQueryWrapper<TeachingClassEntity>().eq(TeachingClassEntity::getId,classId).eq(TeachingClassEntity::getTeacherId,actor.id()))==0)throw new BusinessException(ApiErrorCode.FORBIDDEN,"只能查询自己所授班级的学生",HttpStatus.FORBIDDEN);return memberships.selectClassStudents(classId);}
 public List<TeachingClassView> teacherClasses(AuthenticatedUser actor){
  if(actor.role()!=UserRole.TEACHER)throw new BusinessException(ApiErrorCode.FORBIDDEN,"仅教师可以查询所授班级",HttpStatus.FORBIDDEN);
  return views(mapper.selectList(new LambdaQueryWrapper<TeachingClassEntity>().eq(TeachingClassEntity::getTeacherId,actor.id()).orderByAsc(TeachingClassEntity::getStartDate).orderByAsc(TeachingClassEntity::getId)));
 }
 private TeachingClassView view(TeachingClassEntity entity){return views(List.of(entity)).get(0);}
 private List<TeachingClassView> views(List<TeachingClassEntity> entities){
  if(entities.isEmpty())return List.of();
  Map<Long,String> courseNames=courseMapper.selectBatchIds(entities.stream().map(TeachingClassEntity::getCourseId).distinct().toList()).stream().collect(Collectors.toMap(CourseEntity::getId,CourseEntity::getName));
  Map<Long,String> teacherNames=userMapper.selectBatchIds(entities.stream().map(TeachingClassEntity::getTeacherId).distinct().toList()).stream().collect(Collectors.toMap(UserEntity::getId,u->u.getRealName()==null?u.getUsername():u.getRealName()));
  return entities.stream().map(e->TeachingClassView.from(e,courseNames.get(e.getCourseId()),teacherNames.get(e.getTeacherId()))).toList();
 }
 private TeachingClassEntity requireClass(Long id){TeachingClassEntity e=mapper.selectById(id);if(e==null)throw notFound();return e;}
 private void apply(TeachingClassEntity e,SaveClassRequest r){e.setCourseId(r.courseId());e.setTeacherId(r.teacherId());e.setName(r.name().trim());e.setCapacity(r.capacity());e.setStartDate(r.startDate());e.setEndDate(r.endDate());}
 private void validateDates(LocalDate start,LocalDate end){if(end.isBefore(start))throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"班级结束日期不能早于开始日期");}
 private BusinessException notFound(){return new BusinessException(ApiErrorCode.CLASS_NOT_FOUND,"班级不存在",HttpStatus.NOT_FOUND);}
 private BusinessException invalid(String message){return new BusinessException(ApiErrorCode.INVALID_STATE,message,HttpStatus.CONFLICT);}
}
