package com.educore.enrollment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.enrollment.entity.PaymentRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecordEntity> {
    @Select("SELECT * FROM payment_record WHERE order_id=#{orderId}")
    PaymentRecordEntity selectByOrderId(@Param("orderId") Long orderId);
}
