package com.educore.dashboard.service;

import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.dashboard.entity.vo.ClassAnalyticsView;
import com.educore.dashboard.mapper.ClassAnalyticsMapper;
import com.educore.security.AuthenticatedUser;
import com.educore.user.entity.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassAnalyticsService {
    private final ClassAnalyticsMapper mapper;
    public ClassAnalyticsService(ClassAnalyticsMapper mapper){this.mapper=mapper;}

    @Transactional(readOnly=true)
    public ClassAnalyticsView get(Long classId,AuthenticatedUser actor,boolean adminRoute){
        if(actor==null||(adminRoute?actor.role()!=UserRole.ADMIN:actor.role()!=UserRole.TEACHER))
            throw new BusinessException(ApiErrorCode.FORBIDDEN,"角色无权查看班级分析",HttpStatus.FORBIDDEN);
        ClassAnalyticsMapper.ClassOwner owner=mapper.classOwner(classId);
        if(owner==null)throw new BusinessException(ApiErrorCode.CLASS_NOT_FOUND,"班级不存在",HttpStatus.NOT_FOUND);
        if(!adminRoute&&!owner.teacherId().equals(actor.id()))
            throw new BusinessException(ApiErrorCode.FORBIDDEN,"只能查看自己所授班级的教学分析",HttpStatus.FORBIDDEN);
        var m=mapper.classMetrics(classId);
        return new ClassAnalyticsView(m.classId(),m.className(),m.status(),m.plannedLessons(),m.completedLessons(),
                m.attendanceRate(),m.assignmentCompletionRate(),m.averageScore(),m.highestScore(),m.lowestScore(),
                m.examPassRate(),mapper.exams(classId),mapper.recentStudentResults(classId));
    }
}
