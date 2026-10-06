package com.educore.user.entity.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record RegisterRequest(@NotBlank(message="请输入用户名")
 @Pattern(regexp="[A-Za-z0-9_.-]{3,64}",message="用户名需为 3 到 64 位，只能使用英文字母、数字、点（.）、下划线（_）或短横线（-）") String username,
 @NotBlank(message="请输入密码") @Size(min=8,max=72,message="密码长度需为 8 到 72 位") String password,
 @NotBlank(message="请输入姓名") @Size(max=64,message="姓名不能超过 64 个字符") String realName) { }
