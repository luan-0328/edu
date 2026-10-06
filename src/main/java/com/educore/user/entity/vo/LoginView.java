package com.educore.user.entity.vo;
public record LoginView(String accessToken, String tokenType, long expiresIn, UserView user) { }
