package com.educore.examination.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.course.entity.CourseEntity;
import com.educore.course.entity.enums.CourseStatus;
import com.educore.course.mapper.CourseMapper;
import com.educore.enrollment.mapper.ClassStudentMapper;
import com.educore.examination.entity.dto.*;
import com.educore.examination.entity.*;
import com.educore.examination.entity.enums.*;
import com.educore.examination.mapper.*;
import com.educore.examination.entity.vo.*;
import com.educore.security.AuthenticatedUser;
import com.educore.teachingclass.entity.TeachingClassEntity;
import com.educore.teachingclass.mapper.TeachingClassMapper;
import com.educore.user.entity.enums.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class ExaminationService {
    private final QuestionMapper questions;
    private final ExamMapper exams;
    private final ExamQuestionMapper examQuestions;
    private final ExamAttemptMapper attempts;
    private final ExamAnswerMapper answers;
    private final TeachingClassMapper classes;
    private final CourseMapper courses;
    private final ClassStudentMapper memberships;
    private final ObjectMapper json;

    public ExaminationService(QuestionMapper questions, ExamMapper exams, ExamQuestionMapper examQuestions,
                              ExamAttemptMapper attempts, ExamAnswerMapper answers, TeachingClassMapper classes,
                              CourseMapper courses, ClassStudentMapper memberships, ObjectMapper json) {
        this.questions=questions; this.exams=exams; this.examQuestions=examQuestions; this.attempts=attempts;
        this.answers=answers; this.classes=classes; this.courses=courses; this.memberships=memberships; this.json=json;
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public QuestionView createQuestion(SaveQuestionRequest req, AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER); validateQuestionPayload(req); requireCourse(req.courseId());
        QuestionEntity e=new QuestionEntity(); e.setCourseId(req.courseId()); e.setTeacherId(actor.id()); e.setType(req.type());
        e.setContent(req.content().trim()); e.setOptionsJson(req.optionsJson()); e.setAnswerJson(req.answerJson()); e.setDifficulty(req.difficulty()); e.setStatus(QuestionStatus.ACTIVE);
        questions.insert(e); return questionView(e);
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public QuestionView updateQuestion(Long id, SaveQuestionRequest req, AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER); validateQuestionPayload(req); requireCourse(req.courseId());
        QuestionEntity e=questions.lockQuestion(id); requireQuestion(e);
        if(!e.getTeacherId().equals(actor.id())) throw forbidden("只能修改自己创建的题目");
        e.setCourseId(req.courseId()); e.setType(req.type()); e.setContent(req.content().trim()); e.setOptionsJson(req.optionsJson());
        e.setAnswerJson(req.answerJson()); e.setDifficulty(req.difficulty()); questions.updateById(e); return questionView(e);
    }

    public List<QuestionView> listQuestions(Long courseId, QuestionType type, QuestionDifficulty difficulty, AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER);
        var q=Wrappers.<QuestionEntity>lambdaQuery().eq(QuestionEntity::getTeacherId,actor.id())
                .eq(courseId!=null,QuestionEntity::getCourseId,courseId).eq(type!=null,QuestionEntity::getType,type)
                .eq(difficulty!=null,QuestionEntity::getDifficulty,difficulty).orderByDesc(QuestionEntity::getCreatedAt);
        return questions.selectList(q).stream().map(this::questionView).toList();
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public QuestionView setQuestionStatus(Long id, QuestionStatus status, AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER); QuestionEntity e=questions.lockQuestion(id); requireQuestion(e);
        if(!e.getTeacherId().equals(actor.id())) throw forbidden("只能停用自己创建的题目");
        e.setStatus(status); questions.updateById(e); return questionView(e);
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ExamAdminView createExam(CreateExamRequest req, AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER); requireTimeRange(req.startTime(),req.endTime());
        TeachingClassEntity teachingClass=lockTeachingClass(req.classId(),actor);
        ExamEntity e=new ExamEntity(); e.setClassId(req.classId()); e.setTeacherId(actor.id()); e.setTitle(req.title().trim());
        e.setStartTime(req.startTime());e.setEndTime(req.endTime());e.setDurationMinutes(req.durationMinutes());e.setStatus(ExamStatus.DRAFT);exams.insert(e);
        return examView(e,0);
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ExamAdminView generate(Long examId, GenerateExamRequest req, AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER); ExamEntity exam=exams.lockExam(examId); requireExam(exam);
        requireExamTeacher(exam,actor);
        if(exam.getStatus()!=ExamStatus.DRAFT) throw invalid("已发布考试不能重新组卷");
        if(!examQuestions.listByExam(examId).isEmpty()) throw invalid("试卷已组卷，不能重复组卷");
        TeachingClassEntity teachingClass=classes.selectById(exam.getClassId());
        CourseEntity course=teachingClass==null?null:courses.selectById(teachingClass.getCourseId());
        if(course==null) throw new BusinessException(ApiErrorCode.COURSE_NOT_FOUND,"课程不存在",HttpStatus.NOT_FOUND);
        BigDecimal total=BigDecimal.ZERO;
        for(QuestionRule rule:req.rules()) total=total.add(rule.score().multiply(BigDecimal.valueOf(rule.count())));
        if(total.compareTo(new BigDecimal("100.00"))>0) throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"试卷总分不能超过100分");
        Set<Long> selected=new HashSet<>(); int order=1;
        for(QuestionRule rule:req.rules()) {
            var query=Wrappers.<QuestionEntity>lambdaQuery().eq(QuestionEntity::getCourseId,course.getId())
                    .eq(QuestionEntity::getTeacherId,actor.id()).eq(QuestionEntity::getType,rule.type())
                    .eq(QuestionEntity::getDifficulty,rule.difficulty()).eq(QuestionEntity::getStatus,QuestionStatus.ACTIVE);
            if(!selected.isEmpty()) query.notIn(QuestionEntity::getId,selected);
            List<QuestionEntity> pool=questions.selectList(query);
            if(pool.size()<rule.count()) throw new BusinessException(ApiErrorCode.INVALID_STATE,"符合组卷条件的题目数量不足");
            Collections.shuffle(pool);
            for(QuestionEntity source:pool.subList(0,rule.count())) {
                selected.add(source.getId()); ExamQuestionEntity snap=new ExamQuestionEntity(); snap.setExamId(examId); snap.setSourceQuestionId(source.getId());
                snap.setType(source.getType()); snap.setContentSnapshot(source.getContent()); snap.setOptionsSnapshot(source.getOptionsJson());
                snap.setAnswerSnapshot(source.getAnswerJson()); snap.setDifficulty(source.getDifficulty()); snap.setScore(rule.score()); snap.setSortOrder(order++);
                examQuestions.insert(snap);
            }
        }
        return examView(exam,order-1);
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ExamAdminView publish(Long id, AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER); ExamEntity snapshot=exams.selectById(id);requireExam(snapshot);lockTeachingClass(snapshot.getClassId(),actor);ExamEntity e=exams.lockExam(id); requireExam(e); requireExamTeacher(e,actor);
        if(e.getStatus()!=ExamStatus.DRAFT) throw invalid("只有草稿考试可以发布");
        if(examQuestions.listByExam(id).isEmpty()) throw invalid("请先组卷再发布考试");
        if(!nowUtc().isBefore(e.getEndTime())) throw invalid("考试结束时间已过，不能发布");
        e.setStatus(ExamStatus.PUBLISHED); exams.updateById(e); return examView(e,examQuestions.listByExam(id).size());
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ExamAdminView close(Long id,AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER); ExamEntity e=exams.lockExam(id); requireExam(e); requireExamTeacher(e,actor);
        if(e.getStatus()!=ExamStatus.PUBLISHED) throw invalid("只有已发布考试可以关闭");
        if(attempts.countInProgress(id)>0) throw invalid("仍有学生正在答题，请等待提交或超时收卷后再关闭考试");
        e.setStatus(ExamStatus.CLOSED); exams.updateById(e); return examView(e,examQuestions.listByExam(id).size());
    }

    public List<ExamSummaryView> studentExams(AuthenticatedUser actor) {
        requireRole(actor,UserRole.STUDENT); return exams.listStudentExams(actor.id());
    }

    public List<ExamAdminView> teacherExams(Long classId,AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER);
        var query=Wrappers.<ExamEntity>lambdaQuery().eq(ExamEntity::getTeacherId,actor.id()).eq(classId!=null,ExamEntity::getClassId,classId).orderByDesc(ExamEntity::getStartTime);
        return exams.selectList(query).stream().map(e->examView(e,examQuestions.listByExam(e.getId()).size())).toList();
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ExamStartView start(Long examId, AuthenticatedUser actor) {
        requireRole(actor,UserRole.STUDENT); ExamEntity exam=exams.lockExam(examId); requireExam(exam);
        if(exams.isMember(exam.getClassId(),actor.id())==0) throw forbidden("只能参加本人所在班级的考试");
        LocalDateTime now=nowUtc();
        ExamAttemptEntity attempt=attempts.lockByExamStudent(examId,actor.id());
        if(attempt==null) {
            if(exam.getStatus()!=ExamStatus.PUBLISHED||now.isBefore(exam.getStartTime())||!now.isBefore(exam.getEndTime())) throw invalid("当前不在考试开放时间内");
            attempt=new ExamAttemptEntity(); attempt.setExamId(examId);attempt.setStudentId(actor.id());attempt.setStatus(AttemptStatus.IN_PROGRESS);attempt.setStartedAt(now);attempts.insert(attempt);
        }
        LocalDateTime deadline=attempt.getStartedAt().plusMinutes(exam.getDurationMinutes());
        if(deadline.isAfter(exam.getEndTime())) deadline=exam.getEndTime();
        if(attempt.getStatus()==AttemptStatus.IN_PROGRESS&&!now.isBefore(deadline)) finishAttempt(exam,attempt,deadline);
        Map<Long,JsonNode> draft=new HashMap<>();
        if(attempt.getStatus()==AttemptStatus.IN_PROGRESS) for(ExamAnswerEntity saved:answers.byAttempt(attempt.getId())) if(saved.getAnswerJson()!=null) draft.put(saved.getExamQuestionId(),parseJson(saved.getAnswerJson()));
        return new ExamStartView(examId,attempt.getId(),attempt.getStatus(),attempt.getStartedAt(),deadline,
                examQuestions.listByExam(examId).stream().map(this::studentQuestion).toList(),draft);
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ExamAnswerDraftView saveAnswers(Long examId,SaveExamAnswersRequest req,AuthenticatedUser actor) {
        requireRole(actor,UserRole.STUDENT);ExamEntity exam=exams.selectById(examId);requireExam(exam);
        if(exams.isMember(exam.getClassId(),actor.id())==0)throw forbidden("只能保存本人所在班级的考试答题");
        ExamAttemptEntity attempt=attempts.lockByExamStudent(examId,actor.id());
        if(attempt==null)throw new BusinessException(ApiErrorCode.EXAM_ATTEMPT_NOT_FOUND,"请先开始考试",HttpStatus.NOT_FOUND);
        if(attempt.getStatus()!=AttemptStatus.IN_PROGRESS)throw invalid("答卷已提交，不能继续保存");
        LocalDateTime deadline=attempt.getStartedAt().plusMinutes(exam.getDurationMinutes());if(deadline.isAfter(exam.getEndTime()))deadline=exam.getEndTime();
        if(!nowUtc().isBefore(deadline))throw invalid("考试已截止，不能保存答题");
        List<ExamQuestionEntity> sheet=examQuestions.listByExam(examId);Set<Long> validIds=new HashSet<>();sheet.forEach(q->validIds.add(q.getId()));
        if(req.answers().isEmpty()||!validIds.containsAll(req.answers().keySet()))throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"答题内容不能为空或包含不属于本场试卷的题目");
        for(var entry:req.answers().entrySet()) {
            ExamQuestionEntity question=sheet.stream().filter(q->q.getId().equals(entry.getKey())).findFirst().orElseThrow();
            validateStudentAnswer(question,entry.getValue());
            ExamAnswerEntity row=answers.byAttemptAndQuestion(attempt.getId(),entry.getKey());
            if(row==null){row=new ExamAnswerEntity();row.setAttemptId(attempt.getId());row.setExamQuestionId(entry.getKey());row.setAnswerJson(asJson(entry.getValue()));answers.insert(row);}
            else {row.setAnswerJson(asJson(entry.getValue()));answers.updateById(row);}
        }
        return new ExamAnswerDraftView(attempt.getId(),req.answers().size(),nowUtc());
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ExamSubmissionView submit(Long examId, SubmitExamRequest req, AuthenticatedUser actor) {
        requireRole(actor,UserRole.STUDENT); ExamEntity exam=exams.selectById(examId); requireExam(exam);
        if(exams.isMember(exam.getClassId(),actor.id())==0) throw forbidden("只能提交本人所在班级的考试");
        ExamAttemptEntity attempt=attempts.lockByExamStudent(examId,actor.id());
        if(attempt==null) throw new BusinessException(ApiErrorCode.EXAM_ATTEMPT_NOT_FOUND,"请先开始考试",HttpStatus.NOT_FOUND);
        if(attempt.getStatus()!=AttemptStatus.IN_PROGRESS) return new ExamSubmissionView(attempt.getId(),attempt.getStatus(),attempt.getScore(),attempt.getSubmittedAt());
        LocalDateTime deadline=attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()); if(deadline.isAfter(exam.getEndTime())) deadline=exam.getEndTime();
        if(!nowUtc().isBefore(deadline)) throw invalid("考试已截止，不能提交");
        List<ExamQuestionEntity> sheet=examQuestions.listByExam(examId);
        Set<Long> validIds=new HashSet<>(); sheet.forEach(q->validIds.add(q.getId()));
        if(!validIds.containsAll(req.answers().keySet())) throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"答题内容包含不属于本场试卷的题目");
        BigDecimal objective=BigDecimal.ZERO; boolean subjective=false;
        for(ExamQuestionEntity q:sheet) {
            ExamAnswerEntity row=answers.byAttemptAndQuestion(attempt.getId(),q.getId());
            JsonNode answer=req.answers().containsKey(q.getId())?req.answers().get(q.getId()):(row==null||row.getAnswerJson()==null?null:parseJson(row.getAnswerJson()));
            validateStudentAnswer(q,answer);
            if(row==null){row=new ExamAnswerEntity();row.setAttemptId(attempt.getId());row.setExamQuestionId(q.getId());}
            row.setAnswerJson(asJson(answer));
            if(q.getType()==QuestionType.SHORT_ANSWER) { subjective=true; }
            else { BigDecimal score=matches(q,answer)?q.getScore():BigDecimal.ZERO; row.setScore(score); objective=objective.add(score); }
            if(row.getId()==null)answers.insert(row);else answers.updateById(row);
        }
        attempt.setSubmittedAt(nowUtc()); attempt.setScore(objective); attempt.setStatus(subjective?AttemptStatus.SUBMITTED:AttemptStatus.GRADED); attempts.updateAttempt(attempt);
        return new ExamSubmissionView(attempt.getId(),attempt.getStatus(),attempt.getScore(),attempt.getSubmittedAt());
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ExamSubmissionView gradeAnswer(Long answerId, GradeExamAnswerRequest req, AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER);
        Long attemptId=answers.attemptIdByAnswer(answerId);
        if(attemptId==null) throw new BusinessException(ApiErrorCode.EXAM_QUESTION_NOT_FOUND,"考试答案不存在",HttpStatus.NOT_FOUND);
        ExamAttemptEntity attempt=attempts.lockById(attemptId);
        if(attempt==null) throw new BusinessException(ApiErrorCode.EXAM_ATTEMPT_NOT_FOUND,"考试记录不存在",HttpStatus.NOT_FOUND);
        ExamAnswerEntity answer=answers.lockById(answerId);
        if(answer==null) throw new BusinessException(ApiErrorCode.EXAM_QUESTION_NOT_FOUND,"考试答案不存在",HttpStatus.NOT_FOUND);
        ExamAnswerMapper.GradeAccess access=answers.gradeAccess(answerId);
        if(access==null) throw new BusinessException(ApiErrorCode.EXAM_QUESTION_NOT_FOUND,"考试答案不存在",HttpStatus.NOT_FOUND);
        if(!access.teacherId().equals(actor.id())) throw forbidden("只能批改自己班级的考试");
        if(access.type()!=QuestionType.SHORT_ANSWER) throw invalid("客观题由系统自动判分");
        if(access.attemptStatus()!=AttemptStatus.SUBMITTED) throw invalid("该考试记录当前不能批改");
        if(req.score().compareTo(access.maxScore())>0) throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"得分不能超过该题分值");
        if(answers.grade(answerId,req.score(),req.feedback())!=1) throw invalid("该答案已批改");
        if(answers.ungradedShortAnswers(attempt.getId())==0) {
            attempt.setScore(answers.totalScore(attempt.getId())); attempt.setStatus(AttemptStatus.GRADED); attempts.updateAttempt(attempt);
        }
        return new ExamSubmissionView(attempt.getId(),attempt.getStatus(),attempt.getScore(),attempt.getSubmittedAt());
    }

    public ExamResultView result(Long examId, AuthenticatedUser actor) {
        requireRole(actor,UserRole.STUDENT); ExamEntity exam=exams.selectById(examId); requireExam(exam);
        ExamAttemptEntity attempt=attempts.selectOne(Wrappers.<ExamAttemptEntity>lambdaQuery().eq(ExamAttemptEntity::getExamId,examId).eq(ExamAttemptEntity::getStudentId,actor.id()));
        if(attempt==null) throw new BusinessException(ApiErrorCode.EXAM_ATTEMPT_NOT_FOUND,"考试记录不存在",HttpStatus.NOT_FOUND);
        if(attempt.getStatus()!=AttemptStatus.GRADED) throw invalid("成绩尚未公布");
        return resultView(exam,attempt);
    }

    public List<ExamResultSummaryView> results(Long examId, AuthenticatedUser actor) {
        requireAdminOrTeacher(actor); ExamEntity e=exams.selectById(examId); requireExam(e);
        if(actor.role()==UserRole.TEACHER) requireExamTeacher(e,actor);
        return exams.listResults(examId);
    }

    public List<PendingExamAnswerView> pendingAnswers(Long examId,AuthenticatedUser actor) {
        requireRole(actor,UserRole.TEACHER); ExamEntity e=exams.selectById(examId);requireExam(e);requireExamTeacher(e,actor);
        return answers.pendingSubjectiveAnswers(examId,actor.id());
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public int expireOverdueAttempts(int limit) {
        int completed=0;
        for(Long id:attempts.expiredAttemptIds(Math.max(1,Math.min(limit,500)))) {
            ExamAttemptEntity attempt=attempts.lockById(id);
            if(attempt==null||attempt.getStatus()!=AttemptStatus.IN_PROGRESS) continue;
            ExamEntity exam=exams.selectById(attempt.getExamId()); if(exam==null) continue;
            LocalDateTime deadline=attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()); if(deadline.isAfter(exam.getEndTime())) deadline=exam.getEndTime();
            if(nowUtc().isBefore(deadline)) continue;
            finishAttempt(exam,attempt,deadline); completed++;
        }
        return completed;
    }

    private void finishAttempt(ExamEntity exam,ExamAttemptEntity attempt,LocalDateTime submittedAt) {
            boolean subjective=false;BigDecimal objective=BigDecimal.ZERO;
            for(ExamQuestionEntity q:examQuestions.listByExam(exam.getId())) {
                ExamAnswerEntity row=answers.byAttemptAndQuestion(attempt.getId(),q.getId());
                JsonNode response=row==null||row.getAnswerJson()==null?null:parseJson(row.getAnswerJson());
                if(row==null){row=new ExamAnswerEntity();row.setAttemptId(attempt.getId());row.setExamQuestionId(q.getId());}
                if(row.getAnswerJson()==null)row.setAnswerJson("null");
                if(q.getType()==QuestionType.SHORT_ANSWER)subjective=true;
                else {BigDecimal score=matches(q,response)?q.getScore():BigDecimal.ZERO;row.setScore(score);objective=objective.add(score);}
                if(row.getId()==null)answers.insert(row);else answers.updateById(row);
            }
            attempt.setSubmittedAt(submittedAt); attempt.setScore(objective);
            attempt.setStatus(subjective?AttemptStatus.SUBMITTED:AttemptStatus.GRADED); attempts.updateAttempt(attempt);
    }

    private ExamResultView resultView(ExamEntity exam,ExamAttemptEntity attempt) {
        Map<Long,ExamQuestionEntity> sheet=new HashMap<>(); examQuestions.listByExam(exam.getId()).forEach(q->sheet.put(q.getId(),q));
        List<ExamAnswerResultView> views=answers.selectList(Wrappers.<ExamAnswerEntity>lambdaQuery().eq(ExamAnswerEntity::getAttemptId,attempt.getId()).orderByAsc(ExamAnswerEntity::getId)).stream().map(a->{
            ExamQuestionEntity q=sheet.get(a.getExamQuestionId());
            return new ExamAnswerResultView(a.getId(),q.getId(),q.getType(),q.getContentSnapshot(),q.getOptionsSnapshot(),a.getAnswerJson(),(exam.getStatus()==ExamStatus.CLOSED||!nowUtc().isBefore(exam.getEndTime()))?q.getAnswerSnapshot():null,a.getScore(),q.getScore(),a.getFeedback());
        }).toList();
        return new ExamResultView(exam.getId(),attempt.getId(),attempt.getStudentId(),exam.getTitle(),attempt.getStatus(),attempt.getScore(),attempt.getSubmittedAt(),views);
    }

    private boolean matches(ExamQuestionEntity q,JsonNode actual) {
        if(actual==null||actual.isNull()) return false;
        JsonNode expected=parseJson(q.getAnswerSnapshot());
        if(q.getType()==QuestionType.MULTIPLE_CHOICE&&actual.isArray()&&expected.isArray()) {
            List<String> a=new ArrayList<>(),b=new ArrayList<>(); actual.forEach(n->a.add(n.toString()));expected.forEach(n->b.add(n.toString()));Collections.sort(a);Collections.sort(b);return new HashSet<>(a).equals(new HashSet<>(b));
        }
        return expected.equals(actual);
    }
    private void validateStudentAnswer(ExamQuestionEntity question,JsonNode answer) {
        if(answer==null||answer.isNull())return;
        boolean valid=switch(question.getType()) {
            case SINGLE_CHOICE -> answer.isTextual();
            case MULTIPLE_CHOICE -> answer.isArray()&&answer.size()<=100&&java.util.stream.StreamSupport.stream(answer.spliterator(),false).allMatch(JsonNode::isTextual);
            case TRUE_FALSE -> answer.isBoolean();
            case SHORT_ANSWER -> answer.isTextual()&&answer.textValue().length()<=20000;
        };
        if(!valid)throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"答案格式不正确或简答题内容超过 20000 个字符");
    }
    private void validateQuestionPayload(SaveQuestionRequest req) {
        JsonNode answer=parseJson(req.answerJson());
        if(answer==null||answer.isNull())throw badQuestion("参考答案不能为空");
        if(req.type()==QuestionType.TRUE_FALSE) {
            if(!answer.isBoolean())throw badQuestion("判断题答案必须是布尔值");
            if(req.optionsJson()!=null&&!req.optionsJson().isBlank())parseJson(req.optionsJson());
            return;
        }
        if(req.type()==QuestionType.SHORT_ANSWER) {
            if(!answer.isTextual()||answer.textValue().isBlank())throw badQuestion("简答题参考答案必须是非空字符串");
            return;
        }
        if(req.optionsJson()==null||req.optionsJson().isBlank())throw badQuestion("选择题必须提供选项");
        JsonNode options=parseJson(req.optionsJson());
        if(options==null||!options.isArray()||options.size()<2||options.size()>100)throw badQuestion("选择题需要 2–100 个选项");
        Set<String> ids=new HashSet<>();
        for(JsonNode option:options) {
            String id=null;
            if(option.isTextual())id=option.textValue();
            else if(option.isObject()&&option.path("id").isTextual()&&option.path("text").isTextual()&&!option.path("text").textValue().isBlank())id=option.path("id").textValue();
            if(id==null||id.isBlank()||!ids.add(id))throw badQuestion("选项标识和内容不能为空，标识不能重复");
        }
        if(req.type()==QuestionType.SINGLE_CHOICE) {
            if(!answer.isTextual()||!ids.contains(answer.textValue()))throw badQuestion("单选题答案必须对应一个有效选项");
        } else {
            if(!answer.isArray()||answer.isEmpty())throw badQuestion("多选题答案必须是非空选项数组");
            Set<String> selected=new HashSet<>();
            for(JsonNode selectedAnswer:answer)if(!selectedAnswer.isTextual()||!ids.contains(selectedAnswer.textValue())||!selected.add(selectedAnswer.textValue()))throw badQuestion("多选题答案必须对应有效选项且不能重复");
        }
    }
    private BusinessException badQuestion(String message){return new BusinessException(ApiErrorCode.VALIDATION_ERROR,message);}
    private JsonNode parseJson(String text) {
        try { return json.readTree(text); } catch(Exception e) { throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"题目选项或答案必须是合法 JSON"); }
    }
    private String asJson(JsonNode node) { if(node==null||node.isNull()) return "null"; try{return json.writeValueAsString(node);}catch(JsonProcessingException e){throw new IllegalStateException(e);} }
    private ExamQuestionView studentQuestion(ExamQuestionEntity e){return new ExamQuestionView(e.getId(),e.getType(),e.getContentSnapshot(),e.getOptionsSnapshot(),e.getScore(),e.getSortOrder());}
    private QuestionView questionView(QuestionEntity e){return new QuestionView(e.getId(),e.getCourseId(),e.getTeacherId(),e.getType(),e.getContent(),e.getOptionsJson(),e.getAnswerJson(),e.getDifficulty(),e.getStatus(),e.getCreatedAt());}
    private ExamAdminView examView(ExamEntity e,int count){return new ExamAdminView(e.getId(),e.getClassId(),e.getTeacherId(),e.getTitle(),e.getStartTime(),e.getEndTime(),e.getDurationMinutes(),e.getStatus(),count);}
    private LocalDateTime nowUtc(){return LocalDateTime.now(ZoneOffset.UTC);}
    private void requireTimeRange(LocalDateTime start,LocalDateTime end){if(start==null||end==null||!start.isBefore(end)) throw new BusinessException(ApiErrorCode.VALIDATION_ERROR,"考试开始时间必须早于结束时间");}
    private void requireQuestion(QuestionEntity q){if(q==null) throw new BusinessException(ApiErrorCode.QUESTION_NOT_FOUND,"题目不存在",HttpStatus.NOT_FOUND);}
    private void requireCourse(Long id){if(courses.selectById(id)==null)throw new BusinessException(ApiErrorCode.COURSE_NOT_FOUND,"课程不存在",HttpStatus.NOT_FOUND);}
    private TeachingClassEntity lockTeachingClass(Long classId,AuthenticatedUser actor){
        TeachingClassEntity teachingClass=classes.selectByIdForUpdate(classId);
        if(teachingClass==null)throw new BusinessException(ApiErrorCode.CLASS_NOT_FOUND,"班级不存在",HttpStatus.NOT_FOUND);
        if(!teachingClass.getTeacherId().equals(actor.id()))throw forbidden("只能为自己负责的班级创建和发布考试");
        if(teachingClass.getStatus()!=com.educore.teachingclass.entity.enums.ClassStatus.IN_PROGRESS)throw invalid("只有在教班级可以创建或发布考试");
        return teachingClass;
    }
    private void requireExam(ExamEntity e){if(e==null) throw new BusinessException(ApiErrorCode.EXAM_NOT_FOUND,"考试不存在",HttpStatus.NOT_FOUND);}
    private void requireExamTeacher(ExamEntity e,AuthenticatedUser actor){if(!e.getTeacherId().equals(actor.id())) throw forbidden("只能管理自己负责班级的考试");}
    private void requireRole(AuthenticatedUser u,UserRole role){if(u==null||u.role()!=role) throw forbidden("角色无权执行此操作");}
    private void requireAdminOrTeacher(AuthenticatedUser u){if(u==null||(u.role()!=UserRole.ADMIN&&u.role()!=UserRole.TEACHER)) throw forbidden("仅教师或管理员可查询班级成绩");}
    private BusinessException forbidden(String m){return new BusinessException(ApiErrorCode.FORBIDDEN,m,HttpStatus.FORBIDDEN);}
    private BusinessException invalid(String m){return new BusinessException(ApiErrorCode.INVALID_STATE,m,HttpStatus.CONFLICT);}
    public record ExamSubmissionView(Long attemptId,AttemptStatus status,BigDecimal score,LocalDateTime submittedAt) { }
}
