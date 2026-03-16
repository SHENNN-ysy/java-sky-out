package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@RequestMapping("/admin/dish")
@ApiOperation("菜品管理")
public class DishController {
    @Autowired
    public DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}",dishDTO);
        dishService.save(dishDTO);
        // 清理缓存
        clearCache("dish_"+dishDTO.getCategoryId());
        return Result.success();
    }
    @GetMapping("/page")
    @ApiOperation("分页查询员菜品")
    public Result select(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询菜品参数：{}",dishPageQueryDTO);
        PageResult pageResult= dishService.selectDish(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除菜品：{}",ids);
        // 调用服务层方法执行删除
        dishService.deleteBatch(ids);
        // 清理缓存
        clearCache("dish_*");
        return Result.success();
    }
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品：{}", id);
        DishVO dishVO = dishService.getById(id);
        return Result.success(dishVO);
    }
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品：{}", dishDTO);
        dishService.update(dishDTO);
        // 清理缓存
        clearCache("dish_*");
        return Result.success();
    }
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result selectByCategoryId(@RequestParam Long categoryId){
        log.info("根据分类id查询菜品：{}",categoryId);
        List<Dish> dish = dishService.selectByCategoryId(categoryId);
        return Result.success(dish);
    }
    @PostMapping("/status/{status}")
    @ApiOperation("启售、禁售套餐")
    public Result updateCategoryStatus(@RequestParam Long id,@PathVariable Integer status){
        log.info("启售、禁售菜品;Id:{},Status{}",id,status);
        dishService.updateCategoryStatus(id,status);
        // 清理缓存
        clearCache("dish_*");
        return Result.success();
    }
    //清除缓存
    private  void clearCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
