package com.educore.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("course_cache_invalidation")
public class CourseCacheInvalidationEntity {
    @TableId(type=IdType.INPUT) private Long courseId;
    private Integer attempts;
    private LocalDateTime availableAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
