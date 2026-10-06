package com.educore.course.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.educore.course.entity.enums.CourseStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @TableName("course")
public class CourseEntity {
 @TableId(type=IdType.AUTO) private Long id;
 private String name;
 private String description;
 private BigDecimal price;
 private CourseStatus status;
 @TableField(fill=FieldFill.INSERT) private LocalDateTime createdAt;
 @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
