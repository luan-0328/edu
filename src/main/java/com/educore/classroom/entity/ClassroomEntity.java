package com.educore.classroom.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.educore.classroom.entity.enums.ClassroomStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @TableName("classroom")
public class ClassroomEntity {
 @TableId(type=IdType.AUTO) private Long id;
 private String name;
 private Integer capacity;
 private ClassroomStatus status;
 @TableField(fill=FieldFill.INSERT) private LocalDateTime createdAt;
 @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
