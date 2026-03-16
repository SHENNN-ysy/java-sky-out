package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(@RequestParam Long categoryId) {
        log.info("查询分类下的菜品：{}", categoryId);
        //查询缓存
        List<DishVO> redislist = (List<DishVO>) redisTemplate.opsForValue().get("dish_" + categoryId);
        //若有，则返回缓存数据
        if (redislist != null && redislist.size() > 0) {
            return Result.success(redislist);
        }
        //若没有，则查询数据库，将数据放入缓存，并返回
        DishPageQueryDTO dishPageQueryDTO = new DishPageQueryDTO();
        dishPageQueryDTO.setCategoryId(categoryId.intValue());
        dishPageQueryDTO.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
        dishPageQueryDTO.setPageSize(Integer.MAX_VALUE);
        dishPageQueryDTO.setPage(1);

        List<DishVO> list = dishService.listWithFlavor(dishPageQueryDTO);
        redisTemplate.opsForValue().set("dish_" + categoryId, list);

        return Result.success(list);
    }

}
