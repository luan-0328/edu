package com.educore.config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.educore.user.entity.UserEntity;
import com.educore.user.entity.enums.UserRole;
import com.educore.user.entity.enums.UserStatus;
import com.educore.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component public class BootstrapAdminInitializer implements ApplicationRunner {
 private static final Logger log=LoggerFactory.getLogger(BootstrapAdminInitializer.class);
 private final BootstrapAdminProperties properties;private final UserMapper mapper;private final PasswordEncoder encoder;
 public BootstrapAdminInitializer(BootstrapAdminProperties p,UserMapper m,PasswordEncoder e){properties=p;mapper=m;encoder=e;}
 @Override public void run(ApplicationArguments args){
  if(properties.password()==null||properties.password().isBlank()){log.info("Bootstrap admin skipped: ADMIN_PASSWORD is not configured");return;}
  if(mapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername,properties.username()))>0){log.info("Bootstrap admin already exists");return;}
  UserEntity admin=new UserEntity();admin.setUsername(properties.username());admin.setPasswordHash(encoder.encode(properties.password()));admin.setRealName("系统管理员");admin.setRole(UserRole.ADMIN);admin.setStatus(UserStatus.ACTIVE);mapper.insert(admin);log.info("Bootstrap administrator created from environment configuration");
 }
}
