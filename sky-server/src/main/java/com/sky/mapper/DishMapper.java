package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品数据
     * @param dish
     */
    @Insert("insert into dish (name, category_id, price, status, create_time, update_time, create_user, update_user, image, description) " +
            "values(#{name}, #{categoryId}, #{price}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser}, #{image}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);


    List<DishVO> list(DishPageQueryDTO dishPageQueryDTO);

    Dish selectById(Long id);

    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);

    void deleteByIds(List<Long> idArray);
}
