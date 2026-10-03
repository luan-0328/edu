package com.educore.service.teachingclass;

import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.teachingclass.entity.TeachingClassEntity;
import com.educore.mapper.teachingclass.TeachingClassMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassEnrollmentCapacityService {
    private final TeachingClassMapper mapper;
    public ClassEnrollmentCapacityService(TeachingClassMapper mapper) { this.mapper = mapper; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(Long classId) {
        if (mapper.reserveEnrollmentSeat(classId) == 1) return;
        TeachingClassEntity current = mapper.selectByIdForUpdate(classId);
        if (current == null) throw new BusinessException(ApiErrorCode.CLASS_NOT_FOUND, "班级不存在", HttpStatus.NOT_FOUND);
        if (current.getStatus() != com.educore.teachingclass.enums.ClassStatus.ENROLLING)
            throw new BusinessException(ApiErrorCode.INVALID_STATE, "班级当前未开放报名", HttpStatus.CONFLICT);
        throw new BusinessException(ApiErrorCode.CLASS_FULL, "班级名额已满", HttpStatus.CONFLICT);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Long classId) {
        if (mapper.releaseEnrollmentSeat(classId) != 1)
            throw new IllegalStateException("No reserved seat to release for class " + classId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void confirm(Long classId) {
        if (mapper.confirmEnrollmentSeat(classId) != 1)
            throw new BusinessException(ApiErrorCode.INVALID_STATE, "班级状态已变更，不能完成入班", HttpStatus.CONFLICT);
    }
}
