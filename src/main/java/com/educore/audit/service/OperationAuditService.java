package com.educore.audit.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educore.audit.entity.OperationAuditLogEntity;
import com.educore.audit.mapper.OperationAuditMapper;
import com.educore.common.PageView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationAuditService {
    private final OperationAuditMapper mapper;
    public OperationAuditService(OperationAuditMapper mapper){this.mapper=mapper;}

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void record(Long actorId,String actorRole,String method,String path,int status,String outcome,String requestId) {
        mapper.insertAuditLog(actorId,actorRole,method,path,status,outcome,requestId);
    }

    @Transactional(readOnly=true)
    public PageView<OperationAuditLogEntity> list(long page,long size,Long actorId,String outcome) {
        var query=Wrappers.<OperationAuditLogEntity>lambdaQuery().eq(actorId!=null,OperationAuditLogEntity::getActorId,actorId)
                .eq(outcome!=null&&!outcome.isBlank(),OperationAuditLogEntity::getOutcome,outcome)
                .orderByDesc(OperationAuditLogEntity::getCreatedAt).orderByDesc(OperationAuditLogEntity::getId);
        Page<OperationAuditLogEntity> result=mapper.selectPage(new Page<>(page,size),query);
        return PageView.from(result,result.getRecords());
    }
}
