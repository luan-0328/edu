package com.educore.enrollment.entity.vo;
import java.time.LocalDateTime;
public record ClassMemberView(Long studentId,String username,String realName,LocalDateTime enrolledAt) { }
