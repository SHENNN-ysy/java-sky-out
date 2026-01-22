package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    public void insertBatch(List<DishFlavor>dishFlavorList);

    @Delete("delete from dish_flavor where dish_id = #{id}")
    void deleteByDishId(Long id);

    void deleteByDishIds(List<Long> idArray);

    List<DishFlavor> getByDishId(Long id);
}
