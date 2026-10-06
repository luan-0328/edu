package com.educore.teachingclass.entity.vo;
import com.educore.teachingclass.entity.TeachingClassEntity;
import com.educore.teachingclass.entity.enums.ClassStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record TeachingClassView(Long id,Long courseId,Long teacherId,String name,Integer capacity,Integer reservedCount,Integer enrolledCount,
 LocalDate startDate,LocalDate endDate,ClassStatus status,LocalDateTime createdAt,LocalDateTime updatedAt,String courseName,String teacherName) {
 public static TeachingClassView from(TeachingClassEntity e,String courseName,String teacherName){return new TeachingClassView(e.getId(),e.getCourseId(),e.getTeacherId(),e.getName(),e.getCapacity(),e.getReservedCount(),e.getEnrolledCount(),e.getStartDate(),e.getEndDate(),e.getStatus(),e.getCreatedAt(),e.getUpdatedAt(),courseName,teacherName);}
}
