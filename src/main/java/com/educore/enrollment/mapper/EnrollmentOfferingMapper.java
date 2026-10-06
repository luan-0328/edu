package com.educore.enrollment.mapper;

import com.educore.enrollment.entity.vo.EnrollmentOffering;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EnrollmentOfferingMapper {
    EnrollmentOffering find(@Param("classId") Long classId);
}
