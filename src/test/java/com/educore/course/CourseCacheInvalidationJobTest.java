package com.educore.course;

import com.educore.course.entity.CourseCacheInvalidationEntity;
import com.educore.course.mapper.CourseCacheInvalidationMapper;
import com.educore.course.service.CourseCacheInvalidationJob;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourseCacheInvalidationJobTest {
    @Test void deletesRedisKeyAndOutboxRowAfterSuccessfulInvalidation() {
        CourseCacheInvalidationMapper mapper=mock(CourseCacheInvalidationMapper.class);
        StringRedisTemplate redis=mock(StringRedisTemplate.class);
        CourseCacheInvalidationEntity pending=pending(41L,0);
        when(mapper.claimDue(50)).thenReturn(List.of(pending));
        when(redis.delete("publishedCourses::41")).thenReturn(true);

        new CourseCacheInvalidationJob(mapper,redis).process();

        verify(redis).delete("publishedCourses::41");verify(mapper).deleteById(41L);
        verify(mapper,never()).retry(anyLong(),anyInt(),any(),anyString());
    }

    @Test void schedulesRetryWhenRedisIsUnavailable() {
        CourseCacheInvalidationMapper mapper=mock(CourseCacheInvalidationMapper.class);
        StringRedisTemplate redis=mock(StringRedisTemplate.class);
        when(mapper.claimDue(50)).thenReturn(List.of(pending(42L,2)));
        when(redis.delete("publishedCourses::42")).thenThrow(new IllegalStateException("Redis offline"));

        new CourseCacheInvalidationJob(mapper,redis).process();

        verify(mapper).retry(eq(42L),eq(3),any(LocalDateTime.class),eq("Redis offline"));
        verify(mapper,never()).deleteById(42L);
    }

    private CourseCacheInvalidationEntity pending(Long id,int attempts){
        CourseCacheInvalidationEntity entity=new CourseCacheInvalidationEntity();entity.setCourseId(id);entity.setAttempts(attempts);return entity;
    }
}
