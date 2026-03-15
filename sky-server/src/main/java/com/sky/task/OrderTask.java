package com.sky.task;

import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderService orderService;

    /**
     * 每分钟执行一次，处理超时未支付的订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrderTask() {
        log.info("定时任务：处理超时未支付的订单，时间：{}", LocalDateTime.now());
        orderService.processTimeoutOrders();
    }

    /**
     * 每天凌晨 1 点执行一次，处理派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrderTask() {
        log.info("定时任务：处理派送中的订单，时间：{}", LocalDateTime.now());
        orderService.processDeliveryOrders();
    }
}
