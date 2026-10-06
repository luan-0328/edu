package com.educore.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
@Mapper public interface UserMapper extends BaseMapper<UserEntity> {
 @Select("SELECT * FROM sys_user WHERE id = #{id} FOR UPDATE") UserEntity selectByIdForUpdate(@Param("id") Long id);
 @Select("SELECT COUNT(*) FROM edu_class WHERE teacher_id=#{teacherId} AND status IN ('ENROLLING','IN_PROGRESS')") long countActiveAssignedClasses(@Param("teacherId") Long teacherId);
}
