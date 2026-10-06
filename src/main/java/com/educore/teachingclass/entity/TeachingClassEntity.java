package com.educore.teachingclass.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.educore.teachingclass.entity.enums.ClassStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @TableName("edu_class")
public class TeachingClassEntity {
 @TableId(type=IdType.AUTO) private Long id;
 private Long courseId;
 private Long teacherId;
 private String name;
 private Integer capacity;
 private Integer reservedCount;
 private Integer enrolledCount;
 private LocalDate startDate;
 private LocalDate endDate;
 private ClassStatus status;
 @TableField(fill=FieldFill.INSERT) private LocalDateTime createdAt;
 @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
