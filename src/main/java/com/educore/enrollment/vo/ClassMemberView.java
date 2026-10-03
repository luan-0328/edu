package com.educore.enrollment.vo;
import java.time.LocalDateTime;
public record ClassMemberView(Long studentId,String username,String realName,LocalDateTime enrolledAt) { }
