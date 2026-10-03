package com.educore.course.vo;
import com.educore.course.entity.CourseEntity;
import com.educore.course.enums.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record CourseView(Long id,String name,String description,BigDecimal price,CourseStatus status,LocalDateTime createdAt,LocalDateTime updatedAt) {
 public static CourseView from(CourseEntity e){return new CourseView(e.getId(),e.getName(),e.getDescription(),e.getPrice(),e.getStatus(),e.getCreatedAt(),e.getUpdatedAt());}
}
