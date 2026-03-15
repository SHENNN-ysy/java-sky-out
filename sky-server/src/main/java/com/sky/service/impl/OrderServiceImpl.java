package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.properties.ShopProperties;
import com.sky.properties.WeChatProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private ShopProperties shopProperties;
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //若地址为空，报异常
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if (addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //验证收货地址与店铺距离
        checkDeliveryAddress(addressBook.getProvinceName()+addressBook.getCityName()+addressBook.getDetail());
        //若购物车为空，报异常
        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> carts = shoppingCartMapper.list(cart);
        if (carts==null||carts.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入1条数据
        String address=addressBook.getProvinceName()+addressBook.getCityName()+addressBook.getDetail();
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setUserId(BaseContext.getCurrentId());
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(address);
        orderMapper.insert(orders);
        //向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart cart1 : carts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart1,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //清除购物车数据
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
        //返回数据
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        // 通过 WebSocket 向客户端推送订单状态
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号："+outTradeNo);
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 处理超时未支付的订单
     */
    public void processTimeoutOrders() {
        // 查询 15 分钟前仍未支付的订单
        LocalDateTime orderTime = LocalDateTime.now().minusMinutes(15);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, orderTime);

        if (ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                log.info("发现超时订单，订单号：{},下单时间：{}", orders.getNumber(), orders.getOrderTime());

                // 修改订单状态为已取消
                Orders orderToUpdate = Orders.builder()
                        .id(orders.getId())
                        .status(Orders.CANCELLED)
                        .cancelReason("订单超时未支付，系统自动取消")
                        .cancelTime(LocalDateTime.now())
                        .build();

                orderMapper.update(orderToUpdate);
            }
            log.info("处理完成，共处理超时订单：{} 个", ordersList.size());
        } else {
            log.info("暂无超时未支付订单");
        }
    }

    /**
     * 处理派送中的订单，设置为已完成
     */
    public void processDeliveryOrders() {

        // 查询所有派送中的订单
        List<Orders> ordersList = orderMapper.getByStatus(Orders.DELIVERY_IN_PROGRESS);

        if (ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                log.info("发现派送中订单，订单号：{}, 修改为已完成", orders.getNumber());

                // 修改订单状态为已完成
                Orders orderToUpdate = Orders.builder()
                        .id(orders.getId())
                        .status(Orders.COMPLETED)
                        .deliveryTime(LocalDateTime.now())
                        .build();

                orderMapper.update(orderToUpdate);
            }
            log.info("处理完成，共处理派送中订单：{} 个", ordersList.size());
        } else {
            log.info("暂无派送中订单");
        }
    }

    @Override
    public void reminder(Long id) {
        // 查询订单
        Orders orders = orderMapper.getById(id);
        //检验订单是否存在
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 通过 WebSocket 向客户端推送订单状态
        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "用户催单,订单号："+orders.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 用户端：历史订单分页查询
     */
    @Override
    public PageResult pageQuery(int page, int pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);
        OrdersPageQueryDTO dto = new OrdersPageQueryDTO();
        dto.setUserId(BaseContext.getCurrentId());
        dto.setStatus(status);
        List<Orders> list = orderMapper.pageQuery(dto);
        Page<Orders> p = (Page<Orders>) list;

        //查询订单菜品信息
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : p.getResult()) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);

            // 查询订单详情
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

            // 生成菜品信息字符串：菜品名称*数量,菜品名称*数量...
            StringBuilder sb = new StringBuilder();
            for (OrderDetail orderDetail : orderDetailList) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(orderDetail.getName()).append("*").append(orderDetail.getNumber());
            }
            orderVO.setOrderDishes(sb.toString());
            orderVO.setOrderDetailList(orderDetailList);

            orderVOList.add(orderVO);
        }

        return new PageResult(p.getTotal(), orderVOList);
    }

    /**
     * 查询订单详情
     */
    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);

        // 查询订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 生成菜品信息字符串
        StringBuilder sb = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(orderDetail.getName()).append("*").append(orderDetail.getNumber());
        }
        orderVO.setOrderDishes(sb.toString());
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     */
    @Override
    public void userCancelById(Long id) throws Exception {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有待付款的订单可以取消
        if (!Orders.PENDING_PAYMENT.equals(orders.getStatus())) {
            throw new OrderBusinessException("订单状态不对，不能取消");
        }

        Orders orderToUpdate = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .payStatus(Orders.REFUND)
                .build();

        orderMapper.update(orderToUpdate);
    }

    /**
     * 再来一单
     */
    @Override
    public void repetition(Long id) {
        // 查询历史订单的详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单菜品重新插入购物车
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setDishId(orderDetail.getDishId());
            shoppingCart.setSetmealId(orderDetail.getSetmealId());
            shoppingCart.setNumber(orderDetail.getNumber());
            shoppingCart.setName(orderDetail.getName());
            shoppingCart.setImage(orderDetail.getImage());
            shoppingCart.setAmount(orderDetail.getAmount());
            shoppingCart.setCreateTime(LocalDateTime.now());

            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 管理端：订单条件搜索
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        List<Orders> list = orderMapper.pageQuery(dto);
        Page<Orders> p = (Page<Orders>) list;

        //查询订单菜品信息
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : p.getResult()) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);

            // 查询订单详情
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

            // 生成菜品信息字符串
            StringBuilder sb = new StringBuilder();
            for (OrderDetail orderDetail : orderDetailList) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(orderDetail.getName()).append("*").append(orderDetail.getNumber());
            }
            orderVO.setOrderDishes(sb.toString());
            orderVO.setOrderDetailList(orderDetailList);

            orderVOList.add(orderVO);
        }

        return new PageResult(p.getTotal(), orderVOList);
    }

    /**
     * 订单数量统计
     */
    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO vo = new OrderStatisticsVO();

        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);

        vo.setToBeConfirmed(toBeConfirmed == null ? 0 : toBeConfirmed);
        vo.setConfirmed(confirmed == null ? 0 : confirmed);
        vo.setDeliveryInProgress(deliveryInProgress == null ? 0 : deliveryInProgress);

        return vo;
    }

    /**
     * 接单
     */
    @Override
    public void confirm(OrdersConfirmDTO dto) {
        Orders orders = orderMapper.getById(dto.getId());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有待接单的订单可以接单
        if (!Orders.TO_BE_CONFIRMED.equals(orders.getStatus())) {
            throw new OrderBusinessException("订单状态不对，不能接单");
        }

        Orders orderToUpdate = Orders.builder()
                .id(dto.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orderToUpdate);
    }

    /**
     * 拒单
     */
    @Override
    public void rejection(OrdersRejectionDTO dto) throws Exception {
        Orders orders = orderMapper.getById(dto.getId());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有待接单的订单可以拒单
        if (!Orders.TO_BE_CONFIRMED.equals(orders.getStatus())) {
            throw new OrderBusinessException("订单状态不对，不能拒单");
        }

        // 拒单的同时需要退款
        Orders orderToUpdate = Orders.builder()
                .id(dto.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(dto.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .payStatus(Orders.REFUND)
                .build();

        orderMapper.update(orderToUpdate);
    }

    /**
     * 管理端取消订单
     */
    @Override
    public void cancel(OrdersCancelDTO dto) throws Exception {
        Orders orders = orderMapper.getById(dto.getId());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 已完成、已取消的订单不能取消
        if (Orders.COMPLETED.equals(orders.getStatus()) || Orders.CANCELLED.equals(orders.getStatus())) {
            throw new OrderBusinessException("订单状态不对，不能取消");
        }

        Orders orderToUpdate = Orders.builder()
                .id(dto.getId())
                .status(Orders.CANCELLED)
                .cancelReason(dto.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        // 如果订单已支付，需要退款
        if (Orders.PAID.equals(orders.getPayStatus())) {
            orderToUpdate.setPayStatus(Orders.REFUND);
        }

        orderMapper.update(orderToUpdate);
    }

    /**
     * 派送订单
     */
    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有已接单的订单可以派送
        if (!Orders.CONFIRMED.equals(orders.getStatus())) {
            throw new OrderBusinessException("订单状态不对，不能派送");
        }

        Orders orderToUpdate = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();

        orderMapper.update(orderToUpdate);
    }

    /**
     * 完成订单
     */
    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有派送中的订单可以完成
        if (!Orders.DELIVERY_IN_PROGRESS.equals(orders.getStatus())) {
            throw new OrderBusinessException("订单状态不对，不能完成");
        }

        Orders orderToUpdate = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();

        orderMapper.update(orderToUpdate);
    }

    /**
     * 检验收货地址是否超出配送范围（5公里）
     */
    private void checkDeliveryAddress(String userAddress) {
        log.info("用户收货地址：{}", userAddress);

        // 获取店铺坐标
        Map<String, String> shopMap = new HashMap<>();
        shopMap.put("address", shopProperties.getAddress());
        shopMap.put("output", "json");
        shopMap.put("ak", shopProperties.getAk());

        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", shopMap);
        log.info("店铺坐标：{}", shopCoordinate);

        JSONObject shopObj = JSON.parseObject(shopCoordinate);
        if (shopObj == null || !shopObj.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址查询失败");
        }

        JSONObject shopLocation = shopObj.getJSONObject("result").getJSONObject("location");
        double shopLng = shopLocation.getDouble("lng");
        double shopLat = shopLocation.getDouble("lat");

        // 获取用户地址坐标
        Map<String, String> userMap = new HashMap<>();
        userMap.put("address", userAddress);
        userMap.put("output", "json");
        userMap.put("ak", shopProperties.getAk());

        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", userMap);
        log.info("用户地址坐标：{}", userCoordinate);

        JSONObject userObj = JSON.parseObject(userCoordinate);
        if (userObj == null || !userObj.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址查询失败");
        }

        JSONObject userLocation = userObj.getJSONObject("result").getJSONObject("location");
        double userLng = userLocation.getDouble("lng");
        double userLat = userLocation.getDouble("lat");

        // 计算距离
        Map<String, String> distanceMap = new HashMap<>();
        distanceMap.put("origin", shopLat + "," + shopLng);
        distanceMap.put("destination", userLat + "," + userLng);
        distanceMap.put("type", "riding");
        distanceMap.put("ak", shopProperties.getAk());

        String distanceResult = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/riding", distanceMap);
        log.info("距离查询结果：{}", distanceResult);

        JSONObject distanceObj = JSON.parseObject(distanceResult);
        if (distanceObj == null || !distanceObj.getString("status").equals("0")) {
            throw new OrderBusinessException("距离查询失败");
        }

        JSONArray routesArray = distanceObj.getJSONObject("result").getJSONArray("routes");
        JSONObject route = routesArray.getJSONObject(0);
        int distance = route.getIntValue("distance");
        log.info("店铺与收货地址距离：{}米", distance);

        // 如果距离超过5000米（5公里），则抛出异常
        if (distance > 5000) {
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
