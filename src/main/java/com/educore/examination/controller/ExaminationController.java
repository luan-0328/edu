package com.educore.examination.controller;

import com.educore.common.Result;
import com.educore.examination.dto.*;
import com.educore.examination.enums.*;
import com.educore.examination.service.ExaminationService;
import com.educore.examination.service.ExaminationService.ExamSubmissionView;
import com.educore.examination.vo.*;
import com.educore.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Validated @RequestMapping("/api")
public class ExaminationController {
    private final ExaminationService service;
    public ExaminationController(ExaminationService service){this.service=service;}

    @PostMapping("/teacher/questions") public Result<QuestionView> createQuestion(@Valid @RequestBody SaveQuestionRequest body,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.createQuestion(body,user),rid(r));}
    @PutMapping("/teacher/questions/{id}") public Result<QuestionView> updateQuestion(@PathVariable @Positive Long id,@Valid @RequestBody SaveQuestionRequest body,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.updateQuestion(id,body,user),rid(r));}
    @GetMapping("/teacher/questions") public Result<List<QuestionView>> listQuestions(@RequestParam(required=false) Long courseId,@RequestParam(required=false) QuestionType type,@RequestParam(required=false) QuestionDifficulty difficulty,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.listQuestions(courseId,type,difficulty,user),rid(r));}
    @PatchMapping("/teacher/questions/{id}/status") public Result<QuestionView> questionStatus(@PathVariable @Positive Long id,@Valid @RequestBody QuestionStatusRequest body,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.setQuestionStatus(id,body.status(),user),rid(r));}

    @PostMapping("/teacher/exams") public Result<ExamAdminView> createExam(@Valid @RequestBody CreateExamRequest body,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.createExam(body,user),rid(r));}
    @GetMapping("/teacher/exams") public Result<List<ExamAdminView>> teacherExams(@RequestParam(required=false) Long classId,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.teacherExams(classId,user),rid(r));}
    @PostMapping("/teacher/exams/{id}/generate") public Result<ExamAdminView> generate(@PathVariable @Positive Long id,@Valid @RequestBody GenerateExamRequest body,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.generate(id,body,user),rid(r));}
    @PostMapping("/teacher/exams/{id}/publish") public Result<ExamAdminView> publish(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.publish(id,user),rid(r));}
    @PostMapping("/teacher/exams/{id}/close") public Result<ExamAdminView> close(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.close(id,user),rid(r));}
    @GetMapping("/students/me/exams") public Result<List<ExamSummaryView>> studentExams(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.studentExams(user),rid(r));}
    @PostMapping("/exams/{id}/start") public Result<ExamStartView> start(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.start(id,user),rid(r));}
    @PostMapping("/exams/{id}/submit") public Result<ExamSubmissionView> submit(@PathVariable @Positive Long id,@Valid @RequestBody SubmitExamRequest body,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.submit(id,body,user),rid(r));}
    @PostMapping("/teacher/exam-answers/{id}/grade") public Result<ExamSubmissionView> grade(@PathVariable @Positive Long id,@Valid @RequestBody GradeExamAnswerRequest body,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.gradeAnswer(id,body,user),rid(r));}
    @GetMapping("/exams/{id}/result") public Result<ExamResultView> result(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.result(id,user),rid(r));}
    @GetMapping("/teacher/exams/{id}/results") public Result<List<ExamResultSummaryView>> results(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.results(id,user),rid(r));}
    @GetMapping("/teacher/exams/{id}/answers") public Result<List<PendingExamAnswerView>> pendingAnswers(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.pendingAnswers(id,user),rid(r));}
    @GetMapping("/admin/exams/{id}/results") public Result<List<ExamResultSummaryView>> adminResults(@PathVariable @Positive Long id,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest r){return Result.success(service.results(id,user),rid(r));}
    private String rid(HttpServletRequest r){return String.valueOf(r.getAttribute("requestId"));}
}
