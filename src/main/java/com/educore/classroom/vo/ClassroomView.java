package com.educore.classroom.vo;
import com.educore.classroom.entity.ClassroomEntity;
import com.educore.classroom.enums.ClassroomStatus;
import java.time.LocalDateTime;
public record ClassroomView(Long id,String name,Integer capacity,ClassroomStatus status,LocalDateTime createdAt,LocalDateTime updatedAt) {
 public static ClassroomView from(ClassroomEntity e){return new ClassroomView(e.getId(),e.getName(),e.getCapacity(),e.getStatus(),e.getCreatedAt(),e.getUpdatedAt());}
}
