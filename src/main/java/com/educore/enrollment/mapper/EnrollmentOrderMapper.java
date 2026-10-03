package com.educore.enrollment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.enrollment.entity.EnrollmentOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EnrollmentOrderMapper extends BaseMapper<EnrollmentOrderEntity> {
    @Select("SELECT * FROM enrollment_order WHERE id=#{id} FOR UPDATE")
    EnrollmentOrderEntity selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM enrollment_order WHERE student_id=#{studentId} AND class_id=#{classId} AND status='PENDING' ORDER BY id DESC LIMIT 1")
    EnrollmentOrderEntity selectPending(@Param("studentId") Long studentId, @Param("classId") Long classId);

    @Select("SELECT UTC_TIMESTAMP(3)")
    LocalDateTime currentUtcTime();

    @Select("SELECT id FROM enrollment_order WHERE status='PENDING' AND expire_at<=UTC_TIMESTAMP(3) ORDER BY expire_at,id LIMIT #{limit}")
    List<Long> selectOverdueIds(@Param("limit") int limit);
}
