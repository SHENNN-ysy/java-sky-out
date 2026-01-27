package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

@Service
public class SetmealServiceImpl  implements SetmealService {
    @Autowired
    public SetmealMapper  setmealMapper;
    @Autowired
    public SetmealDishMapper setmealDishMapper;
    @Autowired
    public CategoryMapper categoryMapper;
    @Autowired
    public DishMapper dishMapper;
    @Override
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.save(setmeal);
        Long setmealId = setmeal.getId();
        List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
        if (!CollectionUtils.isEmpty(setmealDishes)){
            setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    @Override
    public PageResult selectSetmeal(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        List<SetmealVO> setmealList = setmealMapper.list(setmealPageQueryDTO);
        Page<SetmealVO> p=(Page<SetmealVO>) setmealList;
        return new PageResult(p.getTotal(),p.getResult());
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.selectById(id);
            if (setmeal.getStatus() == 1) {
                //起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        setmealMapper.deleteBatch(ids);
    }

    @Override
    public SetmealVO selectSetmealById(Long id) {
        Setmeal setmeal = setmealMapper.selectById(id);
        Category category = categoryMapper.selectById(setmeal.getCategoryId());
        List<SetmealDish> setmealDishes = setmealDishMapper.selectBySetmealId(setmeal.getId());
        SetmealVO setmealVO = SetmealVO.builder()
                .id(setmeal.getId())
                .name(setmeal.getName())
                .categoryId(setmeal.getCategoryId())
                .price(setmeal.getPrice())
                .image(setmeal.getImage())
                .description(setmeal.getDescription())
                .status(setmeal.getStatus())
                .categoryName(category.getName())
                .setmealDishes(setmealDishes)
                .build();
        return setmealVO;
    }

    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        setmealDishMapper.deleteByIds(Arrays.asList(setmeal.getId()));
        List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
        if (!CollectionUtils.isEmpty(setmealDishes)){
            setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    @Override
    public void updateSetmealStatus(Long id, Integer status) {
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        //分辨是启售还是停售操作
        if (status==1){
            //若启售，查询套餐中菜品是否停售，若有未启售菜品则不能启售套餐，反之启售
           List<SetmealDish> setmealDishes = setmealDishMapper.selectBySetmealId(id);
           for (SetmealDish setmealDish : setmealDishes) {
               Dish dish = dishMapper.selectById(setmealDish.getDishId());
               if (dish.getStatus()==0){
                   //有未启售的菜品，不能启售
                   throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
               }
           }
           setmealMapper.update(setmeal);
        }
        else {
            //停售菜单
            setmealMapper.update(setmeal);
        }

    }
}
