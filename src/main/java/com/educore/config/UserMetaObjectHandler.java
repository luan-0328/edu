package com.educore.config;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
@Component public class UserMetaObjectHandler implements MetaObjectHandler {
 @Override public void insertFill(MetaObject metaObject){LocalDateTime utcNow=LocalDateTime.now(ZoneOffset.UTC);strictInsertFill(metaObject,"createdAt",LocalDateTime.class,utcNow);strictInsertFill(metaObject,"updatedAt",LocalDateTime.class,utcNow);}
 @Override public void updateFill(MetaObject metaObject){strictUpdateFill(metaObject,"updatedAt",LocalDateTime.class,LocalDateTime.now(ZoneOffset.UTC));}
}
