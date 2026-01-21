package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    void save(DishDTO dishDTO);

    PageResult selectDish(DishPageQueryDTO dishPageQueryDTO);

    void deleteBatch(List<Long> idArray);

    DishVO getById(Long id);

    void update(DishDTO dishDTO);

    List<Dish> selectByCategoryId(Long categoryId);
}
