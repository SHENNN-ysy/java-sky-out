package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);
    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);
    /**
     * 处理超时未支付的订单
     */
    void processTimeoutOrders();

    /**
     * 处理派送中的订单，设置为已完成
     */
    void processDeliveryOrders();

    void reminder(Long id);

    /**
     * 用户端：历史订单分页查询
     */
    PageResult pageQuery(int page, int pageSize, Integer status);

    /**
     * 查询订单详情
     */
    OrderVO details(Long id);

    /**
     * 用户取消订单
     */
    void userCancelById(Long id) throws Exception;

    /**
     * 再来一单
     */
    void repetition(Long id);

    /**
     * 管理端：订单条件搜索
     */
    PageResult conditionSearch(OrdersPageQueryDTO dto);

    /**
     * 订单数量统计
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     */
    void confirm(OrdersConfirmDTO dto);

    /**
     * 拒单
     */
    void rejection(OrdersRejectionDTO dto) throws Exception;

    /**
     * 管理端取消订单
     */
    void cancel(OrdersCancelDTO dto) throws Exception;

    /**
     * 派送订单
     */
    void delivery(Long id);

    /**
     * 完成订单
     */
    void complete(Long id);
}
