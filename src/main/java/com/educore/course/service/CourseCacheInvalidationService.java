package com.educore.course.service;

import com.educore.course.entity.CourseCacheInvalidationEntity;
import com.educore.course.mapper.CourseCacheInvalidationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class CourseCacheInvalidationService {
    private final CourseCacheInvalidationMapper mapper;
    private final boolean cacheEnabled;
    public CourseCacheInvalidationService(CourseCacheInvalidationMapper mapper,@Value("${educore.cache.enabled:true}") boolean cacheEnabled){this.mapper=mapper;this.cacheEnabled=cacheEnabled;}

    @Transactional(propagation=Propagation.MANDATORY)
    public void enqueue(Long courseId) {
        if(!cacheEnabled)return;
        CourseCacheInvalidationEntity pending=mapper.lock(courseId);
        if(pending==null){pending=new CourseCacheInvalidationEntity();pending.setCourseId(courseId);pending.setAttempts(0);pending.setAvailableAt(LocalDateTime.now(ZoneOffset.UTC));mapper.insert(pending);}
        else mapper.requeue(courseId,LocalDateTime.now(ZoneOffset.UTC));
    }
}
