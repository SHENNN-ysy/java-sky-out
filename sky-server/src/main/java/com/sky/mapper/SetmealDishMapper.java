package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface SetmealDishMapper {

    List<Long> getSetmealIdListByDishId(List<Long> id);

    void insertBatch(List<SetmealDish> setmealDishes);
}
