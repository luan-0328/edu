package com.educore.dashboard.service;

import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.dashboard.entity.vo.AdminDashboardView;
import com.educore.dashboard.mapper.DashboardMapper;
import com.educore.security.AuthenticatedUser;
import com.educore.user.entity.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AdminDashboardService {
    private final DashboardMapper mapper;
    public AdminDashboardService(DashboardMapper mapper){this.mapper=mapper;}

    @Transactional(readOnly=true)
    public AdminDashboardView dashboard(AuthenticatedUser actor){
        if(actor==null||actor.role()!=UserRole.ADMIN)
            throw new BusinessException(ApiErrorCode.FORBIDDEN,"仅管理员可以查看运营看板",HttpStatus.FORBIDDEN);
        LocalDate first=LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        LocalDateTime from=first.atStartOfDay(),to=first.plusMonths(1).atStartOfDay();
        return new AdminDashboardView(mapper.totalStudents(),mapper.teachingClassCount(),mapper.currentEnrollmentCount(),
                mapper.paidOrderCount(),mapper.simulatedOrderAmount(),mapper.newEnrollments(from,to),
                mapper.popularCourses(),mapper.classTeaching());
    }
}
