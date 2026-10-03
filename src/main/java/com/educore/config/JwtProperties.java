package com.educore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="educore.jwt")
public record JwtProperties(String secret, long ttlSeconds) { }
