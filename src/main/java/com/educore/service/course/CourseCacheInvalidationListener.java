package com.educore.service.course;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(prefix = "educore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CourseCacheInvalidationListener {
    private static final Logger log = LoggerFactory.getLogger(CourseCacheInvalidationListener.class);
    private final CacheManager cacheManager;
    public CourseCacheInvalidationListener(CacheManager cacheManager) { this.cacheManager = cacheManager; }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void evict(CourseChangedEvent event) {
        try {
            var cache = cacheManager.getCache("publishedCourses");
            if (cache != null) cache.evict(event.courseId());
        } catch (RuntimeException exception) {
            log.warn("Could not evict published course {} from Redis; future reads will use the database", event.courseId(), exception);
        }
    }
}
