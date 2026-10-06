package com.educore.examination.entity.vo;

import java.time.LocalDateTime;

public record ExamAnswerDraftView(Long attemptId,int savedAnswerCount,LocalDateTime savedAt) { }
