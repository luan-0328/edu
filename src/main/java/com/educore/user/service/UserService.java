package com.educore.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.security.JwtService;
import com.educore.user.dto.*;
import com.educore.user.entity.UserEntity;
import com.educore.user.enums.UserRole;
import com.educore.user.enums.UserStatus;
import com.educore.user.mapper.UserMapper;
import com.educore.user.vo.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class UserService {
    private final UserMapper mapper; private final PasswordEncoder encoder; private final JwtService jwt;
    public UserService(UserMapper mapper, PasswordEncoder encoder, JwtService jwt){this.mapper=mapper;this.encoder=encoder;this.jwt=jwt;}

    @Transactional
    public UserView register(RegisterRequest request) { return UserView.from(create(request.username(),request.password(),request.realName(),UserRole.STUDENT)); }
    public LoginView login(LoginRequest request) {
        UserEntity user=mapper.selectOne(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername,request.username()));
        if(user==null || !encoder.matches(request.password(),user.getPasswordHash())) throw new BusinessException(ApiErrorCode.INVALID_CREDENTIALS,"用户名或密码错误",HttpStatus.UNAUTHORIZED);
        if(user.getStatus()!=UserStatus.ACTIVE) throw new BusinessException(ApiErrorCode.USER_DISABLED,"账号已被禁用",HttpStatus.FORBIDDEN);
        return new LoginView(jwt.issue(user),"Bearer",jwt.ttlSeconds(),UserView.from(user));
    }
    public UserView getCurrent(Long id){return UserView.from(requireUser(id));}
    @Transactional public UserView updateProfile(Long id, UpdateProfileRequest request){ UserEntity user=requireUser(id);user.setRealName(request.realName().trim());mapper.updateById(user);return UserView.from(user); }
    @Transactional public UserView createTeacher(CreateTeacherRequest request){return UserView.from(create(request.username(),request.password(),request.realName(),UserRole.TEACHER));}
    public Page<UserView> listUsers(long page,long size){
        Page<UserEntity> result=mapper.selectPage(new Page<>(page,size),new LambdaQueryWrapper<UserEntity>().orderByDesc(UserEntity::getId));
        Page<UserView> view=new Page<>(result.getCurrent(),result.getSize(),result.getTotal());view.setRecords(result.getRecords().stream().map(UserView::from).toList());return view;
    }
    @Transactional public UserView updateStatus(Long id,UserStatus status,Long operatorId){
        UserEntity target=mapper.selectByIdForUpdate(id);if(target==null)throw new BusinessException(ApiErrorCode.USER_NOT_FOUND,"用户不存在",HttpStatus.NOT_FOUND);
        if(target.getRole()==UserRole.ADMIN && target.getId().equals(operatorId) && status==UserStatus.DISABLED) throw new BusinessException(ApiErrorCode.FORBIDDEN,"不能禁用当前管理员账号",HttpStatus.FORBIDDEN);
        target.setStatus(status);mapper.updateById(target);return UserView.from(target);
    }
    public UserEntity requireActiveUser(Long id){UserEntity user=requireUser(id);if(user.getStatus()!=UserStatus.ACTIVE)throw new BusinessException(ApiErrorCode.USER_DISABLED,"账号已被禁用",HttpStatus.UNAUTHORIZED);return user;}
    @Transactional(propagation=Propagation.MANDATORY)
    public UserEntity lockActiveTeacher(Long id){
        UserEntity user=lockTeacherRecord(id);
        if(user.getRole()!=UserRole.TEACHER || user.getStatus()!=UserStatus.ACTIVE)
            throw new BusinessException(ApiErrorCode.RESOURCE_UNAVAILABLE,"教师不存在或未启用",HttpStatus.BAD_REQUEST);
        return user;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public UserEntity lockActiveStudent(Long id){
        UserEntity user=mapper.selectByIdForUpdate(id);
        if(user==null || user.getRole()!=UserRole.STUDENT || user.getStatus()!=UserStatus.ACTIVE)
            throw new BusinessException(ApiErrorCode.RESOURCE_UNAVAILABLE,"学生账号不存在或未启用",HttpStatus.FORBIDDEN);
        return user;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public UserEntity lockTeacherRecord(Long id){UserEntity user=mapper.selectByIdForUpdate(id);if(user==null)throw new BusinessException(ApiErrorCode.RESOURCE_UNAVAILABLE,"教师不存在",HttpStatus.BAD_REQUEST);return user;}
    private UserEntity requireUser(Long id){UserEntity user=mapper.selectById(id);if(user==null)throw new BusinessException(ApiErrorCode.USER_NOT_FOUND,"用户不存在",HttpStatus.NOT_FOUND);return user;}
    private UserEntity create(String username,String password,String realName,UserRole role){
        if(mapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername,username))>0)throw new BusinessException(ApiErrorCode.USERNAME_TAKEN,"用户名已存在",HttpStatus.CONFLICT);
        UserEntity user=new UserEntity();user.setUsername(username);user.setPasswordHash(encoder.encode(password));user.setRealName(realName.trim());user.setRole(role);user.setStatus(UserStatus.ACTIVE);
        try{mapper.insert(user);}catch(DuplicateKeyException ex){throw new BusinessException(ApiErrorCode.USERNAME_TAKEN,"用户名已存在",HttpStatus.CONFLICT);} return user;
    }
}
