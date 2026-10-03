package com.educore.service.teachingclass;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.course.entity.CourseEntity;
import com.educore.course.enums.CourseStatus;
import com.educore.service.course.CourseService;
import com.educore.security.AuthenticatedUser;
import com.educore.service.schedule.ScheduleService;
import com.educore.teachingclass.dto.SaveClassRequest;
import com.educore.teachingclass.dto.ClassStatusRequest;
import com.educore.teachingclass.entity.TeachingClassEntity;
import com.educore.teachingclass.enums.ClassStatus;
import com.educore.mapper.teachingclass.TeachingClassMapper;
import com.educore.teachingclass.vo.TeachingClassView;
import com.educore.user.entity.UserEntity;
import com.educore.user.enums.UserRole;
import com.educore.service.user.UserService;
import com.educore.mapper.enrollment.ClassStudentMapper;
import com.educore.enrollment.vo.ClassMemberView;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educore.common.PageView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TreeSet;

@Service public class TeachingClassService {
 private final TeachingClassMapper mapper;private final CourseService courses;private final UserService users;private final ScheduleService schedules;private final ClassStudentMapper memberships;
 public TeachingClassService(TeachingClassMapper mapper,CourseService courses,UserService users,ScheduleService schedules,ClassStudentMapper memberships){this.mapper=mapper;this.courses=courses;this.users=users;this.schedules=schedules;this.memberships=memberships;}
 @Transactional public TeachingClassView create(SaveClassRequest request){
  validateDates(request.startDate(),request.endDate());courses.lockCourse(request.courseId());users.lockActiveTeacher(request.teacherId());
  TeachingClassEntity e=new TeachingClassEntity();apply(e,request);e.setReservedCount(0);e.setEnrolledCount(0);e.setStatus(ClassStatus.DRAFT);mapper.insert(e);return TeachingClassView.from(e);
 }
 @Transactional public TeachingClassView update(Long id,SaveClassRequest request){
  TeachingClassEntity snapshot=requireClass(id);if(snapshot.getStatus()!=ClassStatus.DRAFT&&snapshot.getStatus()!=ClassStatus.ENROLLING)throw invalid("仅草稿或报名中的班级可以修改");
  validateDates(request.startDate(),request.endDate());
  TreeSet<Long> courseIds=new TreeSet<>(List.of(snapshot.getCourseId(),request.courseId()));for(Long courseId:courseIds)courses.lockCourse(courseId);
  TreeSet<Long> teacherIds=new TreeSet<>(List.of(snapshot.getTeacherId(),request.teacherId()));for(Long teacherId:teacherIds)users.lockTeacherRecord(teacherId);users.lockActiveTeacher(request.teacherId());
  TeachingClassEntity e=mapper.selectByIdForUpdate(id);if(e==null)throw notFound();
  if(e.getStatus()!=snapshot.getStatus()||!e.getCourseId().equals(snapshot.getCourseId())||!e.getTeacherId().equals(snapshot.getTeacherId()))throw invalid("班级已被其他操作修改，请重新读取后再试");
  if(request.capacity()<e.getReservedCount()+e.getEnrolledCount())throw new BusinessException(ApiErrorCode.CAPACITY_EXCEEDED,"班级容量不能小于已占用名额",HttpStatus.CONFLICT);
  if(e.getStatus()==ClassStatus.ENROLLING&&(!e.getCourseId().equals(request.courseId())||!e.getTeacherId().equals(request.teacherId()))&&(e.getReservedCount()>0||e.getEnrolledCount()>0||mapper.countScheduledLessons(id)>0))throw invalid("班级已有有效名额或课次，不能更换课程或教师");
  if(mapper.countSchedulesWithSmallRoom(id,request.capacity())>0)throw new BusinessException(ApiErrorCode.CAPACITY_EXCEEDED,"班级容量不能大于已有课次使用教室的容量",HttpStatus.CONFLICT);
  if(mapper.countSchedulesOutsideDates(id,request.startDate().atStartOfDay(),request.endDate().plusDays(1).atStartOfDay())>0)throw invalid("新教学日期范围与已排课次不匹配");
  apply(e,request);mapper.updateById(e);return TeachingClassView.from(e);
 }
 @Transactional public TeachingClassView updateStatus(Long id,ClassStatus target){
  TeachingClassEntity snapshot=requireClass(id);
  courses.lockCourse(snapshot.getCourseId());users.lockTeacherRecord(snapshot.getTeacherId());
  TeachingClassEntity e=mapper.selectByIdForUpdate(id);if(e==null)throw notFound();
  if(e.getStatus()!=snapshot.getStatus())throw invalid("班级状态已被其他操作修改，请重新读取后再试");
  ClassStatus from=e.getStatus();if(from==target)return TeachingClassView.from(e);
  LocalDate today=LocalDate.now(ZoneOffset.UTC);
  boolean valid=switch(target){
   case ENROLLING->from==ClassStatus.DRAFT;
   case IN_PROGRESS->from==ClassStatus.ENROLLING&& !today.isBefore(e.getStartDate());
   case FINISHED->from==ClassStatus.IN_PROGRESS&&today.isAfter(e.getEndDate());
   case CANCELLED->(from==ClassStatus.DRAFT||from==ClassStatus.ENROLLING)&&today.isBefore(e.getStartDate())&&e.getReservedCount()==0&&e.getEnrolledCount()==0;
   case DRAFT->false;
  };
  if(!valid)throw invalid("不允许的班级状态变更或班级尚未满足变更条件");
  if(target==ClassStatus.ENROLLING||target==ClassStatus.IN_PROGRESS)users.lockActiveTeacher(e.getTeacherId());
  if(target==ClassStatus.ENROLLING){CourseEntity course=courses.lockCourse(e.getCourseId());if(course.getStatus()!=CourseStatus.PUBLISHED)throw invalid("课程上架后才能开放班级报名");}
  e.setStatus(target);mapper.updateById(e);if(target==ClassStatus.CANCELLED)schedules.cancelScheduledForClass(id);return TeachingClassView.from(e);
 }
 public TeachingClassView detail(Long id){return TeachingClassView.from(requireClass(id));}
 public PageView<TeachingClassView> listAdmin(long page,long size){Page<TeachingClassEntity> p=mapper.selectPage(new Page<>(page,size),new LambdaQueryWrapper<TeachingClassEntity>().orderByDesc(TeachingClassEntity::getCreatedAt).orderByDesc(TeachingClassEntity::getId));return PageView.from(p,p.getRecords().stream().map(TeachingClassView::from).toList());}
 public List<TeachingClassView> openByCourse(Long courseId){return mapper.selectList(new LambdaQueryWrapper<TeachingClassEntity>().eq(TeachingClassEntity::getCourseId,courseId).eq(TeachingClassEntity::getStatus,ClassStatus.ENROLLING).orderByAsc(TeachingClassEntity::getStartDate)).stream().map(TeachingClassView::from).toList();}
 public List<ClassMemberView> students(Long classId,AuthenticatedUser actor){if(actor==null||actor.role()!=UserRole.TEACHER)throw new BusinessException(ApiErrorCode.FORBIDDEN,"仅教师可以查询班级学生",HttpStatus.FORBIDDEN);if(mapper.selectCount(new LambdaQueryWrapper<TeachingClassEntity>().eq(TeachingClassEntity::getId,classId).eq(TeachingClassEntity::getTeacherId,actor.id()))==0)throw new BusinessException(ApiErrorCode.FORBIDDEN,"只能查询自己所授班级的学生",HttpStatus.FORBIDDEN);return memberships.selectClassStudents(classId);}
 public List<TeachingClassView> teacherClasses(AuthenticatedUser actor){
  if(actor.role()!=UserRole.TEACHER)throw new BusinessException(ApiErrorCode.FORBIDDEN,"仅教师可以查询所授班级",HttpStatus.FORBIDDEN);
  return mapper.selectList(new LambdaQueryWrapper<TeachingClassEntity>().eq(TeachingClassEntity::getTeacherId,actor.id()).orderByDesc(TeachingClassEntity::getStartDate).orderByDesc(TeachingClassEntity::getId)).stream().map(TeachingClassView::from).toList();
 }
 private TeachingClassEntity requireClass(Long id){TeachingClassEntity e=mapper.selectById(id);if(e==null)throw notFound();return e;}
 private void apply(TeachingClassEntity e,SaveClassRequest r){e.setCourseId(r.courseId());e.setTeacherId(r.teacherId());e.setName(r.name().trim());e.setCapacity(r.capacity());e.setStartDate(r.startDate());e.setEndDate(r.endDate());}
 private void validateDates(LocalDate start,LocalDate end){if(end.isBefore(start))throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"班级结束日期不能早于开始日期");}
 private BusinessException notFound(){return new BusinessException(ApiErrorCode.CLASS_NOT_FOUND,"班级不存在",HttpStatus.NOT_FOUND);}
 private BusinessException invalid(String message){return new BusinessException(ApiErrorCode.INVALID_STATE,message,HttpStatus.CONFLICT);}
}
