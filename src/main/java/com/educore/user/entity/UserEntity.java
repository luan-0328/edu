package com.educore.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.educore.user.entity.enums.UserRole;
import com.educore.user.entity.enums.UserStatus;
import java.time.LocalDateTime;

@TableName("sys_user")
public class UserEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String username;
    private String passwordHash;
    private String realName;
    private UserRole role;
    private UserStatus status;
    @TableField(fill=FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public String getRealName(){return realName;} public void setRealName(String v){realName=v;}
    public UserRole getRole(){return role;} public void setRole(UserRole v){role=v;}
    public UserStatus getStatus(){return status;} public void setStatus(UserStatus v){status=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
