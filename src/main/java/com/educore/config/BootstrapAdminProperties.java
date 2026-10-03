package com.educore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="educore.bootstrap-admin")
public record BootstrapAdminProperties(String username, String password) { }
