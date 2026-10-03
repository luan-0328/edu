package com.educore.service.course;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educore.common.*;
import com.educore.course.dto.SaveCourseRequest;
import com.educore.course.entity.CourseEntity;
import com.educore.course.enums.CourseStatus;
import com.educore.mapper.course.CourseMapper;
import com.educore.course.vo.CourseView;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service public class CourseService {
 private final CourseMapper mapper; private final ApplicationEventPublisher events;
 public CourseService(CourseMapper mapper,ApplicationEventPublisher events){this.mapper=mapper;this.events=events;}
 @Transactional public CourseView create(SaveCourseRequest request){CourseEntity e=new CourseEntity();apply(e,request);e.setStatus(CourseStatus.DRAFT);mapper.insert(e);events.publishEvent(new CourseChangedEvent(e.getId()));return CourseView.from(e);}
 @Transactional public CourseView update(Long id,SaveCourseRequest request){CourseEntity e=lockCourse(id);apply(e,request);mapper.updateById(e);events.publishEvent(new CourseChangedEvent(id));return CourseView.from(e);}
 @Transactional public CourseView updateStatus(Long id,CourseStatus status){
  CourseEntity e=lockCourse(id);if(e.getStatus()==status)return CourseView.from(e);
  boolean valid=switch(e.getStatus()){case DRAFT->status==CourseStatus.PUBLISHED;case PUBLISHED->status==CourseStatus.OFFLINE;case OFFLINE->status==CourseStatus.PUBLISHED;};
  if(!valid)throw new BusinessException(ApiErrorCode.INVALID_STATE,"不允许的课程状态变更",HttpStatus.CONFLICT);
  if(status==CourseStatus.OFFLINE && mapper.countOpenClasses(id)>0)throw new BusinessException(ApiErrorCode.INVALID_STATE,"仍有报名中的教学班，不能下架课程",HttpStatus.CONFLICT);
  e.setStatus(status);mapper.updateById(e);events.publishEvent(new CourseChangedEvent(id));return CourseView.from(e);
 }
 public PageView<CourseView> listPublished(long page,long size){Page<CourseEntity> p=mapper.selectPage(new Page<>(page,size),new LambdaQueryWrapper<CourseEntity>().eq(CourseEntity::getStatus,CourseStatus.PUBLISHED).orderByDesc(CourseEntity::getCreatedAt).orderByDesc(CourseEntity::getId));return PageView.from(p,p.getRecords().stream().map(CourseView::from).toList());}
 public PageView<CourseView> listAdmin(long page,long size){Page<CourseEntity> p=mapper.selectPage(new Page<>(page,size),new LambdaQueryWrapper<CourseEntity>().orderByDesc(CourseEntity::getCreatedAt).orderByDesc(CourseEntity::getId));return PageView.from(p,p.getRecords().stream().map(CourseView::from).toList());}
 @Cacheable(cacheNames="publishedCourses",key="#id") public CourseView detailPublished(Long id){CourseEntity e=mapper.selectOne(new LambdaQueryWrapper<CourseEntity>().eq(CourseEntity::getId,id).eq(CourseEntity::getStatus,CourseStatus.PUBLISHED));if(e==null)throw notFound();return CourseView.from(e);}
 @Transactional(propagation=Propagation.MANDATORY) public CourseEntity lockCourse(Long id){CourseEntity e=mapper.selectByIdForUpdate(id);if(e==null)throw notFound();return e;}
 private BusinessException notFound(){return new BusinessException(ApiErrorCode.COURSE_NOT_FOUND,"课程不存在",HttpStatus.NOT_FOUND);}
 private void apply(CourseEntity e,SaveCourseRequest r){e.setName(r.name().trim());e.setDescription(r.description()==null?null:r.description().trim());e.setPrice(r.price());}
}
