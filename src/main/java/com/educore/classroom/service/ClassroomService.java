package com.educore.classroom.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.educore.classroom.dto.SaveClassroomRequest;
import com.educore.classroom.dto.ClassroomStatusRequest;
import com.educore.classroom.entity.ClassroomEntity;
import com.educore.classroom.enums.ClassroomStatus;
import com.educore.classroom.mapper.ClassroomMapper;
import com.educore.classroom.vo.ClassroomView;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class ClassroomService {
 private final ClassroomMapper mapper;public ClassroomService(ClassroomMapper mapper){this.mapper=mapper;}
 @Transactional public ClassroomView create(SaveClassroomRequest request){ClassroomEntity e=new ClassroomEntity();e.setName(request.name().trim());e.setCapacity(request.capacity());e.setStatus(ClassroomStatus.ACTIVE);mapper.insert(e);return ClassroomView.from(e);}
 @Transactional public ClassroomView update(Long id,SaveClassroomRequest request){ClassroomEntity e=lockClassroom(id);if(mapper.countOversizedScheduledClasses(id,request.capacity())>0)throw new BusinessException(ApiErrorCode.CAPACITY_EXCEEDED,"教室容量低于已排课班级的容量",HttpStatus.CONFLICT);e.setName(request.name().trim());e.setCapacity(request.capacity());mapper.updateById(e);return ClassroomView.from(e);}
 @Transactional public ClassroomView updateStatus(Long id,ClassroomStatus status){ClassroomEntity e=lockClassroom(id);e.setStatus(status);mapper.updateById(e);return ClassroomView.from(e);}
 public List<ClassroomView> list(){return mapper.selectList(new LambdaQueryWrapper<ClassroomEntity>().orderByAsc(ClassroomEntity::getName).orderByAsc(ClassroomEntity::getId)).stream().map(ClassroomView::from).toList();}
 @Transactional(propagation=Propagation.MANDATORY) public ClassroomEntity lockActiveClassroom(Long id){ClassroomEntity e=lockClassroomRecord(id);if(e.getStatus()!=ClassroomStatus.ACTIVE)throw new BusinessException(ApiErrorCode.RESOURCE_UNAVAILABLE,"教室不存在或未启用",HttpStatus.BAD_REQUEST);return e;}
 @Transactional(propagation=Propagation.MANDATORY) public ClassroomEntity lockClassroomRecord(Long id){ClassroomEntity e=mapper.selectByIdForUpdate(id);if(e==null)throw new BusinessException(ApiErrorCode.RESOURCE_UNAVAILABLE,"教室不存在",HttpStatus.BAD_REQUEST);return e;}
 private ClassroomEntity lockClassroom(Long id){ClassroomEntity e=mapper.selectByIdForUpdate(id);if(e==null)throw new BusinessException(ApiErrorCode.CLASSROOM_NOT_FOUND,"教室不存在",HttpStatus.NOT_FOUND);return e;}
}
