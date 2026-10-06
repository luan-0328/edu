package com.educore.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.course.entity.CourseCacheInvalidationEntity;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CourseCacheInvalidationMapper extends BaseMapper<CourseCacheInvalidationEntity> {
    @Select("SELECT * FROM course_cache_invalidation WHERE course_id=#{courseId} FOR UPDATE")
    CourseCacheInvalidationEntity lock(@Param("courseId") Long courseId);
    @Update("UPDATE course_cache_invalidation SET attempts=0,available_at=#{availableAt},last_error=NULL,updated_at=CURRENT_TIMESTAMP(3) WHERE course_id=#{courseId}")
    int requeue(@Param("courseId") Long courseId,@Param("availableAt") LocalDateTime availableAt);
    @Select("SELECT * FROM course_cache_invalidation WHERE available_at<=CURRENT_TIMESTAMP(3) ORDER BY available_at,course_id LIMIT #{limit} FOR UPDATE SKIP LOCKED")
    List<CourseCacheInvalidationEntity> claimDue(@Param("limit") int limit);
    @Update("UPDATE course_cache_invalidation SET attempts=#{attempts},available_at=#{availableAt},last_error=#{error},updated_at=CURRENT_TIMESTAMP(3) WHERE course_id=#{courseId}")
    int retry(@Param("courseId") Long courseId,@Param("attempts") int attempts,@Param("availableAt") LocalDateTime availableAt,@Param("error") String error);
}
