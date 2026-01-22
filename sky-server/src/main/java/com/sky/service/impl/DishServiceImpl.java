package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Employee;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
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
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    public DishMapper dishMapper;
    @Autowired
    public DishFlavorMapper dishFlavorMapper;
    @Autowired
    public SetmealDishMapper setmealDishMapper;
    @Autowired
    public CategoryMapper categoryMapper;
    @Override
    @Transactional
    public void save(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);
        Long dishId = dish.getId();
        List<DishFlavor>dishFlavorList =dishDTO.getFlavors();
        if (!CollectionUtils.isEmpty(dishFlavorList)){
            dishFlavorList.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(dishFlavorList);
        }

    }

    @Override
    public PageResult selectDish(DishPageQueryDTO dishPageQueryDTO) {
        //1. 设置PageHelper分页参数
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        //2. 执行查询
        List<DishVO> dishList = dishMapper.list(dishPageQueryDTO);
        //3. 封装分页结果
        Page<DishVO> p = (Page<DishVO>)dishList;
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public void deleteBatch(List<Long> idArray) {
        for (Long id : idArray) {
            Dish dish = dishMapper.selectById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                //起售中的菜品不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
            List<Long> setmealIdList = setmealDishMapper.getSetmealIdListByDishId(idArray);
            if (setmealIdList != null && setmealIdList.size() > 0) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }
        }
//        for (Long id : idArray) {
//            dishMapper.deleteById(id);
//            dishFlavorMapper.deleteByDishId(id);
//        }
        dishMapper.deleteByIds(idArray);
        dishFlavorMapper.deleteByDishIds(idArray);
    }

    @Override
    public DishVO getById(Long id) {
        // 查询菜品基本信息
        Dish dish = dishMapper.selectById(id);

        // 查询菜品口味信息
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);

        // 查询分类名称
        Category category = categoryMapper.selectById(dish.getCategoryId());

        // 构建返回对象
        DishVO dishVO = DishVO.builder()
                .id(dish.getId())
                .name(dish.getName())
                .categoryId(dish.getCategoryId())
                .price(dish.getPrice())
                .image(dish.getImage())
                .description(dish.getDescription())
                .status(dish.getStatus())
                .updateTime(dish.getUpdateTime())
                .categoryName(category != null ? category.getName() : "")
                .flavors(flavors)
                .build();

        return dishVO;
    }

    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        // 更新菜品基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        // 删除原有口味信息
        dishFlavorMapper.deleteByDishIds(Arrays.asList(dish.getId()));

        // 插入新的口味信息
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (!CollectionUtils.isEmpty(flavors)) {
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dish.getId()));
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public List<Dish> selectByCategoryId(Long categoryId) {
        List<Dish> dish = dishMapper.selectByCategoryId(categoryId);
        return dish;
    }

}

