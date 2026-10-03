package com.educore.teachingclass.vo;
import com.educore.teachingclass.entity.TeachingClassEntity;
import com.educore.teachingclass.enums.ClassStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record TeachingClassView(Long id,Long courseId,Long teacherId,String name,Integer capacity,Integer reservedCount,Integer enrolledCount,
 LocalDate startDate,LocalDate endDate,ClassStatus status,LocalDateTime createdAt,LocalDateTime updatedAt) {
 public static TeachingClassView from(TeachingClassEntity e){return new TeachingClassView(e.getId(),e.getCourseId(),e.getTeacherId(),e.getName(),e.getCapacity(),e.getReservedCount(),e.getEnrolledCount(),e.getStartDate(),e.getEndDate(),e.getStatus(),e.getCreatedAt(),e.getUpdatedAt());}
}
