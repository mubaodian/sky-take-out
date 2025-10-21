package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;


    /**
     * 新增菜品
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        //向菜品表中插入1条数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);

        //获取菜品id,赋值给口味的dishId属性
        Long id = dish.getId();

        //向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()){
            flavors.forEach(flavor -> flavor.setDishId(id));
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.page(dishPageQueryDTO);
        long total = page.getTotal();
        List<DishVO> result = page.getResult();
        return new PageResult(total,result);
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否在起售中
        List<Long> sellingDishIds = dishMapper.getSellingDishByIds(ids);
        if(sellingDishIds.size() != 0){
            throw new DeletionNotAllowedException("删除失败，菜品id为：" + sellingDishIds + MessageConstant.DISH_ON_SALE);
        }
        //判断当前菜品是否被套餐关联
        List<Long> setmealWithDish = setmealDishMapper.getCountByDishIds(ids);
        if(setmealWithDish.size() != 0){
            throw new DeletionNotAllowedException("删除失败，菜品id为：" + setmealWithDish + MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //要么全部删除，要么全部都不删除（回滚）

        //删除菜品
        dishMapper.deleteByIds(ids);
        //删除菜品对应的口味数据
        dishFlavorMapper.deleteByDishIds(ids);

    }

    /**
     * 菜品起售、停售
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish d = Dish.builder()
                .id(id)
                .status(status)
                .build();

        dishMapper.update(d);
    }

    /**
     * 根据id查询菜品和口味
     * @param id
     * @return
     */
    @Override
    public DishVO getById(Long id) {
        //根据id查询菜品
        Dish d = dishMapper.getById(id);
        //根据id查询口味
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
        //封装到DishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(d,dishVO);
        dishVO.setFlavors(flavors);
        return dishVO;
    }

    /**
     * 更新菜品
     * @param dishDTO
     */
    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        //更新菜品
        Dish d = new Dish();
        BeanUtils.copyProperties(dishDTO,d);
        dishMapper.update(d);
        //删除原来的口味
        dishFlavorMapper.deleteByDishIds(Collections.singletonList(dishDTO.getId()));
        //更新口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()){
            flavors.forEach(flavor -> flavor.setDishId(dishDTO.getId()));
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据分类id查询菜品(用户端的口味)
     * @param categoryId
     * @return
     */
    @Override
    public List<DishVO> list(Long categoryId) {
        List<DishVO> dishVOList = dishMapper.list(categoryId);
        for(DishVO d: dishVOList){
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());
            d.setFlavors(flavors);
        }
        return dishVOList;
    }
}
