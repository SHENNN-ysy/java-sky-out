package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    List<ShoppingCart> list(ShoppingCart cart);

    void update(ShoppingCart cartInDB);

    @Insert("insert into shopping_cart (name, image, dish_id, setmeal_id, dish_flavor, number, create_time, user_id,amount) " +
            "values (#{name}, #{image}, #{dishId}, #{setmealId},#{dishFlavor}, #{number}, #{createTime}, #{userId}, #{amount})")
    void insert(ShoppingCart cart);

    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);
}
