package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/admin/setmeal")
@ApiOperation("套餐管理")
public class SetmealController {
    @Autowired
    public SetmealService setmealService;
    @PostMapping
    @ApiOperation("新增套餐")
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐：{}",setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }
    @GetMapping("/page")
    @ApiOperation("分页查询套餐")
    public Result select( SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询套餐：{}",setmealPageQueryDTO);
        PageResult pageResult= setmealService.selectSetmeal(setmealPageQueryDTO);
        return Result.success(pageResult);
    }
    @DeleteMapping
    @ApiOperation("批量删除套餐")
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除套餐：{}",ids);
        if (ids==null || ids.size()==0){
            return Result.error("请选择要删除的套餐");
        }
        // 调用服务层方法执行删除
        setmealService.deleteBatch(ids);
        return Result.success();
    }
    @PutMapping
    @ApiOperation("修改套餐")
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐：{}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result select(@PathVariable Long id){
        log.info("根据id查询套餐：{}",id);
        com.sky.vo.SetmealVO setmealVO = setmealService.selectSetmealById(id);
        return Result.success(setmealVO);
    }
    @PostMapping("/status/{status}")
    @ApiOperation("启售、禁售套餐")
    public Result updateSetmealStatus(@RequestParam Long id,@PathVariable Integer status){
        log.info("启售、禁售套餐;Id:{},Status{}",id,status);
        setmealService.updateSetmealStatus(id,status);
        return Result.success();
    }
}
