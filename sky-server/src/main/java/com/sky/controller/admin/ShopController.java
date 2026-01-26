package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@Slf4j
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
public class ShopController {
    @Autowired
    RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    @ApiOperation("设置营业状态")
    public Result setStatus( @PathVariable Integer status){
        log.info("设置店铺营业状态:{}",status==1?"营业中":"打烊中");
        //调用redis 服务端字符串存储店铺营业状态
        redisTemplate.opsForValue().set("SHOP_STATUS",status);
        return Result.success();
    }
    @GetMapping("/status")
    @ApiOperation("admin查询营业状态")
    public Result getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
        log.info("查询店铺营业状态:{}",status==1?"营业中":"打烊中");
        return Result.success(status);
    }
}
