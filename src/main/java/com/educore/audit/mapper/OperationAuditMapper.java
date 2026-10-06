package com.educore.audit.mapper;

import com.educore.audit.entity.OperationAuditLogEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface OperationAuditMapper extends BaseMapper<OperationAuditLogEntity> {
    @Insert("INSERT INTO operation_audit_log(actor_id,actor_role,http_method,request_path,response_status,outcome,request_id) VALUES(#{actorId},#{actorRole},#{method},#{path},#{status},#{outcome},#{requestId})")
    int insertAuditLog(@Param("actorId") Long actorId,@Param("actorRole") String actorRole,@Param("method") String method,
               @Param("path") String path,@Param("status") int status,@Param("outcome") String outcome,
               @Param("requestId") String requestId);
}
