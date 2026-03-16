package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.EmployeeMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO){
        //判读当前加入购物车的商品是否已经存在
        ShoppingCart cart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,cart);
        cart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(cart);
        //若存在，则商品数量加一
        if(list != null && list.size() > 0){
            ShoppingCart cartInDB = list.get(0);
            cartInDB.setNumber(cartInDB.getNumber() + 1);
            shoppingCartMapper.update(cartInDB);
        }
        //若不存在，则添加一条数据进数据库
        else {
            if (cart.getDishId()!=null){
                Dish dish = dishMapper.selectById(cart.getDishId());
                cart.setName(dish.getName());
                cart.setImage(dish.getImage());
                cart.setAmount(dish.getPrice());
            }
            else if (cart.getSetmealId()!=null){
                Setmeal setmeal = setmealMapper.selectById(cart.getSetmealId());
                cart.setName(setmeal.getName());
                cart.setImage(setmeal.getImage());
                cart.setAmount(setmeal.getPrice());
            }
            cart.setCreateTime(LocalDateTime.now());
            cart.setNumber(1);
            shoppingCartMapper.insert(cart);
        }
    }
    public List<ShoppingCart> getShoppingCartList(){
        Long userId = BaseContext.getCurrentId();
        ShoppingCart cart = ShoppingCart.builder()
                .userId(userId)
                .build();
        return shoppingCartMapper.list(cart);
    }
    public void clear(){
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }
}
