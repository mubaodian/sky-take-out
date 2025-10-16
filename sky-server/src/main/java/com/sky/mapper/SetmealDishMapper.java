package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id查询是否有关联套餐的菜品id
     * @param ids
     * @return
     */
    List<Long> getCountByDishIds(List<Long> ids);
}
