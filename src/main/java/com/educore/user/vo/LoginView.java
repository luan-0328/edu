package com.educore.user.vo;
public record LoginView(String accessToken, String tokenType, long expiresIn, UserView user) { }
