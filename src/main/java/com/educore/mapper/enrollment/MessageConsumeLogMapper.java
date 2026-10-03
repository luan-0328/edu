package com.educore.mapper.enrollment;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessageConsumeLogMapper {
    @Insert("INSERT IGNORE INTO message_consume_log(consumer_name,event_id) VALUES(#{consumer},#{eventId})")
    int insertOnce(@Param("consumer") String consumer, @Param("eventId") Long eventId);
}
