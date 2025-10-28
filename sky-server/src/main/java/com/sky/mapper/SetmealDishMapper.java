package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id查询是否有关联套餐的菜品id
     * @param ids
     * @return
     */
    List<Long> getCountByDishIds(List<Long> ids);

    /**
     * 批量新增套餐菜品关系
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id批量删除套餐菜品关系
     * @param ids
     */
    void deleteBatchBySetmealIds(List<Long> ids);

    /**
     * 根据套餐id查询套餐菜品关系
     * @param id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getBySetmealId(Long id);
}
