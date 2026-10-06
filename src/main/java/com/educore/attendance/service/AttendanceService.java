package com.educore.attendance.service;

import com.educore.attendance.entity.dto.BatchAttendanceRequest;
import com.educore.attendance.mapper.AttendanceMapper;
import com.educore.attendance.entity.vo.AttendanceView;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.security.AuthenticatedUser;
import com.educore.user.entity.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;

@Service
public class AttendanceService {
    private final AttendanceMapper mapper;
    public AttendanceService(AttendanceMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void record(Long scheduleId, BatchAttendanceRequest request, AuthenticatedUser actor) {
        requireRole(actor, UserRole.TEACHER);
        AttendanceMapper.ScheduleAccess schedule = mapper.scheduleAccessForUpdate(scheduleId);
        if (schedule == null) throw new BusinessException(ApiErrorCode.SCHEDULE_NOT_FOUND, "课次不存在", HttpStatus.NOT_FOUND);
        if (!schedule.teacherId().equals(actor.id())) throw forbidden("只能记录自己所授班级的考勤");
        if (schedule.status() != com.educore.schedule.entity.enums.ScheduleStatus.SCHEDULED)
            throw new BusinessException(ApiErrorCode.INVALID_STATE,"课次已确认完成，不能修改考勤",HttpStatus.CONFLICT);
        var ids = request.records().stream().map(r -> r.studentId()).toList();
        if (new HashSet<>(ids).size() != ids.size()) throw new BusinessException(ApiErrorCode.VALIDATION_ERROR, "同一批次不能重复包含学生");
        for (Long id : ids) if (mapper.isEnrolled(schedule.classId(), id) == 0) throw forbidden("考勤学生不属于该班级");
        mapper.upsertBatch(scheduleId, request.records());
    }

    public List<AttendanceView> mine(AuthenticatedUser actor) {
        requireRole(actor, UserRole.STUDENT);
        return mapper.studentAttendance(actor.id());
    }

    public List<com.educore.attendance.entity.vo.AttendanceRosterView> scheduleRoster(Long scheduleId,AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER);
        AttendanceMapper.ScheduleAccess schedule=mapper.scheduleAccess(scheduleId);
        if(schedule==null) throw new BusinessException(ApiErrorCode.SCHEDULE_NOT_FOUND,"课次不存在",HttpStatus.NOT_FOUND);
        if(!schedule.teacherId().equals(actor.id())) throw forbidden("只能查看自己所授班级的考勤");
        return mapper.scheduleRoster(scheduleId);
    }

    private void requireRole(AuthenticatedUser actor, UserRole role) {
        if (actor == null || actor.role() != role) throw forbidden("角色无权执行此操作");
    }
    private BusinessException forbidden(String message) { return new BusinessException(ApiErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN); }
}
