package com.educore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "educore.enrollment")
public class EnrollmentProperties {
    private long orderTtlMinutes = 30;
    public long getOrderTtlMinutes() { return orderTtlMinutes; }
    public void setOrderTtlMinutes(long value) { this.orderTtlMinutes = value; }
}
