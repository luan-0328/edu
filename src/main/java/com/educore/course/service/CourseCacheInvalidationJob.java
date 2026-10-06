package com.educore.course.service;

import com.educore.course.mapper.CourseCacheInvalidationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@ConditionalOnProperty(prefix="educore.cache",name="enabled",havingValue="true",matchIfMissing=true)
public class CourseCacheInvalidationJob {
    private static final Logger log=LoggerFactory.getLogger(CourseCacheInvalidationJob.class);
    private final CourseCacheInvalidationMapper mapper;
    private final StringRedisTemplate redis;
    public CourseCacheInvalidationJob(CourseCacheInvalidationMapper mapper,StringRedisTemplate redis){this.mapper=mapper;this.redis=redis;}

    @Scheduled(fixedDelayString="${educore.cache.invalidation-poll-ms:5000}")
    @Transactional
    public void process() {
        for(var pending:mapper.claimDue(50)) {
            try {
                redis.delete("publishedCourses::"+pending.getCourseId());
                mapper.deleteById(pending.getCourseId());
            } catch(RuntimeException failure) {
                int attempts=(pending.getAttempts()==null?0:pending.getAttempts())+1;
                long delay=Math.min(300,1L<<Math.min(attempts,8));
                String message=failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage();
                if(message.length()>500)message=message.substring(0,500);
                mapper.retry(pending.getCourseId(),attempts,LocalDateTime.now(ZoneOffset.UTC).plusSeconds(delay),message);
                log.warn("Course cache invalidation will retry for course {} (attempt {})",pending.getCourseId(),attempts);
            }
        }
    }
}
