package com.educore.dashboard.entity.vo;

import com.educore.schedule.entity.vo.ScheduleView;
import java.util.List;

public record TeacherDashboardView(int responsibleClassCount,int inProgressClassCount,
                                   long pendingAssignmentCount,long pendingExamAnswerCount,
                                   List<ScheduleView> upcomingSchedules,
                                   List<TeacherPendingAssignmentView> pendingAssignments,
                                   List<TeacherPendingExamView> pendingExams,
                                   List<TeacherClassProgressView> classProgress) { }
