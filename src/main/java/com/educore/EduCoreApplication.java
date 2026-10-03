package com.educore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EduCoreApplication {
    public static void main(String[] args) { SpringApplication.run(EduCoreApplication.class, args); }
}
