package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单详情
     * @param orderDetails
     */
    void insertBatch(List<OrderDetail> orderDetails);
}
