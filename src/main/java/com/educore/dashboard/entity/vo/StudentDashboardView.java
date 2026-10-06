package com.educore.dashboard.entity.vo;

import com.educore.assignment.entity.vo.AssignmentView;
import com.educore.enrollment.entity.vo.StudentClassView;
import com.educore.examination.entity.vo.ExamSummaryView;
import java.util.List;

public record StudentDashboardView(List<StudentClassView> enrolledClasses,
                                   List<StudentClassProgressView> classProgress,
                                   NextLessonView nextLesson,
                                   List<AssignmentView> upcomingAssignments,
                                   List<ExamSummaryView> upcomingExams,
                                   List<StudentExamResultView> recentResults,
                                   long unreadNotificationCount) { }
