package com.educore.assignment.service;

import com.educore.assignment.dto.*;
import com.educore.assignment.entity.*;
import com.educore.assignment.enums.AssignmentStatus;
import com.educore.assignment.enums.SubmissionStatus;
import com.educore.assignment.mapper.AssignmentMapper;
import com.educore.assignment.vo.*;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.enrollment.service.MessageOutboxService;
import com.educore.security.AuthenticatedUser;
import com.educore.user.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssignmentService {
    private final AssignmentMapper mapper;
    private final MessageOutboxService outbox;
    public AssignmentService(AssignmentMapper mapper, MessageOutboxService outbox) { this.mapper = mapper; this.outbox = outbox; }

    @Transactional
    public AssignmentAdminView create(SaveAssignmentRequest req, AuthenticatedUser actor) {
        requireRole(actor, UserRole.TEACHER);
        requireOwnClass(req.classId(), actor.id());
        validateDeadline(req.deadline());
        AssignmentEntity e = new AssignmentEntity(); e.setClassId(req.classId()); e.setTeacherId(actor.id());
        e.setTitle(req.title().trim()); e.setContent(req.content()); e.setDeadline(req.deadline()); e.setStatus(AssignmentStatus.DRAFT);
        mapper.insert(e); return view(e);
    }

    @Transactional
    public AssignmentAdminView update(Long id, SaveAssignmentRequest req, AuthenticatedUser actor) {
        requireRole(actor, UserRole.TEACHER);
        AssignmentEntity e = mapper.lockAssignment(id); requireAssignment(e);
        if (!e.getTeacherId().equals(actor.id())) throw forbidden("只能修改自己班级的作业");
        if (e.getStatus() != AssignmentStatus.DRAFT) throw invalid("只有草稿作业可以修改");
        requireOwnClass(req.classId(), actor.id()); validateDeadline(req.deadline());
        e.setClassId(req.classId()); e.setTitle(req.title().trim()); e.setContent(req.content()); e.setDeadline(req.deadline());
        mapper.updateById(e); return view(e);
    }

    @Transactional
    public AssignmentAdminView publish(Long id, AuthenticatedUser actor) {
        requireRole(actor, UserRole.TEACHER);
        AssignmentEntity e = mapper.lockAssignment(id); requireAssignment(e);
        if (!e.getTeacherId().equals(actor.id())) throw forbidden("只能发布自己班级的作业");
        if (e.getStatus() != AssignmentStatus.DRAFT) throw invalid("只有草稿作业可以发布");
        if (!e.getDeadline().isAfter(LocalDateTime.now(java.time.ZoneOffset.UTC))) throw invalid("作业截止时间必须晚于当前时间");
        e.setStatus(AssignmentStatus.PUBLISHED); mapper.updateById(e);
        outbox.enqueueAssignmentPublished(e);
        return view(e);
    }

    @Transactional
    public AssignmentAdminView close(Long id, AuthenticatedUser actor) {
        requireRole(actor, UserRole.TEACHER);
        AssignmentEntity e = mapper.lockAssignment(id); requireAssignment(e);
        if (!e.getTeacherId().equals(actor.id())) throw forbidden("只能关闭自己班级的作业");
        if (e.getStatus() != AssignmentStatus.PUBLISHED) throw invalid("只有已发布作业可以关闭");
        e.setStatus(AssignmentStatus.CLOSED); mapper.updateById(e); return view(e);
    }

    public List<AssignmentView> mine(AuthenticatedUser actor) {
        requireRole(actor, UserRole.STUDENT); return mapper.listStudentAssignments(actor.id());
    }

    public List<AssignmentAdminView> classAssignments(Long classId,AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER); requireOwnClass(classId,actor.id());
        return mapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<AssignmentEntity>lambdaQuery()
                .eq(AssignmentEntity::getClassId,classId).eq(AssignmentEntity::getTeacherId,actor.id()).orderByDesc(AssignmentEntity::getCreatedAt))
                .stream().map(this::view).toList();
    }

    @Transactional
    public SubmissionView submit(Long id, SubmitAssignmentRequest req, AuthenticatedUser actor) {
        requireRole(actor, UserRole.STUDENT);
        AssignmentEntity a = mapper.lockAssignment(id); requireAssignment(a);
        if (a.getStatus() != AssignmentStatus.PUBLISHED) throw invalid("作业未开放提交");
        if (mapper.isMember(a.getClassId(), actor.id()) == 0) throw forbidden("只能提交本人所在班级的作业");
        if (!LocalDateTime.now().isBefore(a.getDeadline())) throw invalid("作业已截止");
        AssignmentSubmissionEntity existing = mapper.lockSubmission(id, actor.id());
        if (existing == null) {
            AssignmentSubmissionEntity row = new AssignmentSubmissionEntity(); row.setAssignmentId(id); row.setStudentId(actor.id()); row.setContent(req.content());
            row.setStatus(SubmissionStatus.SUBMITTED); row.setSubmittedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
            mapper.insertSubmission(row); return submissionView(row);
        }
        if (existing.getStatus() == SubmissionStatus.GRADED) throw invalid("作业已批改，不能修改提交");
        existing.setContent(req.content()); existing.setSubmittedAt(LocalDateTime.now(java.time.ZoneOffset.UTC)); mapper.updateSubmission(existing); return submissionView(existing);
    }

    public List<SubmissionProgressView> progress(Long id, AuthenticatedUser actor) {
        requireRole(actor, UserRole.TEACHER);
        AssignmentEntity a = mapper.selectById(id); requireAssignment(a);
        if (!a.getTeacherId().equals(actor.id())) throw forbidden("只能查看自己班级的作业提交");
        return mapper.listProgress(a.getClassId(), id);
    }

    @Transactional
    public SubmissionView grade(Long id, GradeSubmissionRequest req, AuthenticatedUser actor) {
        requireRole(actor, UserRole.TEACHER);
        AssignmentSubmissionEntity s = mapper.lockSubmissionById(id);
        if (s == null) throw new BusinessException(ApiErrorCode.SUBMISSION_NOT_FOUND, "作业提交不存在", HttpStatus.NOT_FOUND);
        AssignmentEntity a = mapper.selectById(s.getAssignmentId()); requireAssignment(a);
        if (!a.getTeacherId().equals(actor.id())) throw forbidden("只能批改自己班级的作业");
        if (s.getStatus() != SubmissionStatus.SUBMITTED) throw invalid("该作业已批改");
        s.setScore(req.score()); s.setFeedback(req.feedback()); s.setStatus(SubmissionStatus.GRADED); s.setGradedAt(LocalDateTime.now());
        mapper.updateSubmission(s); return submissionView(s);
    }

    private void requireOwnClass(Long classId, Long teacherId) {
        if (mapper.ownsClass(classId, teacherId) == 0) throw forbidden("班级不存在或不属于当前教师");
    }
    private AssignmentAdminView view(AssignmentEntity e) { return new AssignmentAdminView(e.getId(),e.getClassId(),e.getTeacherId(),e.getTitle(),e.getContent(),e.getDeadline(),e.getStatus()); }
    private SubmissionView submissionView(AssignmentSubmissionEntity e) { return new SubmissionView(e.getId(),e.getAssignmentId(),e.getStudentId(),e.getContent(),e.getStatus(),e.getScore(),e.getFeedback(),e.getSubmittedAt(),e.getGradedAt()); }
    private void validateDeadline(LocalDateTime deadline) { if (deadline == null) throw new BusinessException(ApiErrorCode.VALIDATION_ERROR, "截止时间不能为空"); }
    private void requireAssignment(AssignmentEntity e) { if (e == null) throw new BusinessException(ApiErrorCode.ASSIGNMENT_NOT_FOUND, "作业不存在", HttpStatus.NOT_FOUND); }
    private void requireRole(AuthenticatedUser actor, UserRole role) { if (actor == null || actor.role() != role) throw forbidden("角色无权执行此操作"); }
    private BusinessException forbidden(String m) { return new BusinessException(ApiErrorCode.FORBIDDEN, m, HttpStatus.FORBIDDEN); }
    private BusinessException invalid(String m) { return new BusinessException(ApiErrorCode.INVALID_STATE, m, HttpStatus.CONFLICT); }
}
