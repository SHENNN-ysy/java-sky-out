package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface SetmealDishMapper {

    List<Long> getSetmealIdListByDishId(List<Long> id);
}
