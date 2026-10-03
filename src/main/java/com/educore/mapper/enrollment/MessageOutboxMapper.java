package com.educore.mapper.enrollment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.enrollment.entity.MessageOutboxEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface MessageOutboxMapper extends BaseMapper<MessageOutboxEntity> {
    @Select("SELECT * FROM message_outbox WHERE status IN ('PENDING','RETRY','PROCESSING') AND available_at<=UTC_TIMESTAMP(3) AND (locked_until IS NULL OR locked_until<=UTC_TIMESTAMP(3)) ORDER BY id LIMIT #{limit} FOR UPDATE SKIP LOCKED")
    List<MessageOutboxEntity> claimable(@Param("limit") int limit);

    @Update("UPDATE message_outbox SET status='PROCESSING',attempts=attempts+1,locked_until=TIMESTAMPADD(SECOND,60,UTC_TIMESTAMP(3)),last_error=NULL WHERE id=#{id}")
    int markProcessing(@Param("id") Long id);

    @Update("UPDATE message_outbox SET status='SENT',published_at=UTC_TIMESTAMP(3),locked_until=NULL,last_error=NULL WHERE id=#{id} AND status='PROCESSING'")
    int markSent(@Param("id") Long id);

    @Update("UPDATE message_outbox SET status='RETRY',available_at=TIMESTAMPADD(SECOND,#{delaySeconds},UTC_TIMESTAMP(3)),locked_until=NULL,last_error=#{error} WHERE id=#{id} AND status='PROCESSING'")
    int markRetry(@Param("id") Long id, @Param("delaySeconds") int delaySeconds, @Param("error") String error);
}
