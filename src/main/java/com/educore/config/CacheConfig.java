package com.educore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.educore.course.entity.vo.CourseView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import java.time.Duration;

@Configuration @EnableCaching
@ConditionalOnProperty(prefix = "educore.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig implements CachingConfigurer {
    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean CacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper cacheMapper = new ObjectMapper().findAndRegisterModules();
        Jackson2JsonRedisSerializer<CourseView> courseSerializer = new Jackson2JsonRedisSerializer<>(cacheMapper, CourseView.class);
        RedisSerializer<Object> serializer = new RedisSerializer<>() {
            @Override public byte[] serialize(Object value) {
                if (value == null) return null;
                if (!(value instanceof CourseView course)) throw new IllegalArgumentException("Unexpected cached value type: " + value.getClass());
                return courseSerializer.serialize(course);
            }
            @Override public Object deserialize(byte[] bytes) {
                return bytes == null ? null : courseSerializer.deserialize(bytes);
            }
        };
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(factory).cacheDefaults(config).build();
    }

    @Bean @Override public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) { log.warn("Cache read failed for {}:{}; using database", cache.getName(), key, exception); }
            @Override public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) { log.warn("Cache write failed for {}:{}", cache.getName(), key, exception); }
            @Override public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) { log.warn("Cache eviction failed for {}:{}", cache.getName(), key, exception); }
            @Override public void handleCacheClearError(RuntimeException exception, Cache cache) { log.warn("Cache clear failed for {}", cache.getName(), exception); }
        };
    }
}
