package com.educore.mapper.enrollment;

import com.educore.enrollment.vo.EnrollmentOffering;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EnrollmentOfferingMapper {
    @Select("SELECT ec.id AS class_id,ec.course_id,c.price,c.status AS course_status,ec.status AS class_status,ec.start_date,ec.end_date " +
            "FROM edu_class ec JOIN course c ON c.id=ec.course_id WHERE ec.id=#{classId}")
    EnrollmentOffering find(@Param("classId") Long classId);
}
