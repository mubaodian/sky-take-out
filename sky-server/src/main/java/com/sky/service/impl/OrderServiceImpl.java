package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
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
    private UserMapper userMapper;

    @Override
    @Transactional
    /**
     * 提交订单
     */
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //判断地址簿是否为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            //说明用户没有填地址
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //查询当前用户的购物车数据是否为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if(list == null || list.size() == 0){
            //说明用户没有添加购物车数据
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone() );
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        orderMapper.insert(orders);

        List<OrderDetail> orderDetails = new ArrayList<>();
        //向订单明细表插入n条数据
        for(ShoppingCart cart : list){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetail.setId(null);

            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        //用户支付订单，应该要清空购物车里的数据
        shoppingCartMapper.delete(shoppingCart);

        //返回VO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
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
        /*JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );*/
        JSONObject jsonObject = new JSONObject();

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
    @Override
    @Transactional
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
    }

    /**
     * 历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(OrdersPageQueryDTO ordersPageQueryDTO) {
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Result result = getResult(ordersPageQueryDTO);
        return new PageResult(result.page().getTotal(), result.list());
    }

    //查询订单通用方法
    private Result getResult(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.page(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        for(Orders orders : page){
            OrderVO orderVO = new OrderVO();

            Long orderId = orders.getId();
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

            BeanUtils.copyProperties(orders, orderVO);
            orderVO.setOrderDetailList(orderDetailList);

            //String的orderDishes
            List<String> orderDishesStr = orderDetailList.stream().map(x -> {
                String orderDish = x.getName() + "*" + x.getNumber() + ";";
                return orderDish;
            }).collect(Collectors.toList());
            orderVO.setOrderDishes(String.join("", orderDishesStr));
            list.add(orderVO);
        }
        Result result = new Result(page, list);
        return result;
    }

    private record Result(Page<Orders> page, List<OrderVO> list) {
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO orderDetail(Long id) {
        OrderVO orderVO = new OrderVO();

        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetail = orderDetailMapper.getByOrderId(id);

        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetail);
        return orderVO;
    }

    /**
     * 用户取消订单
     * @param id
     */
    @Override
    public void cancel(Long id){
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if(orders.getStatus() > 2){
            //该订单状态不能取消
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //退款
            log.info("退款成功");
            orders.setPayStatus(Orders.REFUND);
        }
        //如果还没有支付
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason("用户取消");

        orderMapper.update(orders);

    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        if(orderDetails == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        for(OrderDetail od : orderDetails){
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(od, shoppingCart,"id");
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());

            shoppingCartMapper.insert(shoppingCart);
        }

    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        Result result = getResult(ordersPageQueryDTO);
        return new PageResult(result.page().getTotal(), result.list());
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statisticsVOResult() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();

        Integer tobeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer Confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        orderStatisticsVO.setToBeConfirmed(tobeConfirmed);
        orderStatisticsVO.setConfirmed(Confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = orderMapper.getById(ordersConfirmDTO.getId());
        //用户支付了，才能接单
        if(!orders.getPayStatus().equals(Orders.PAID)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());

        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //只有处于待接单状态(2)的订单才能拒单
        if(!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //如果用户的支付状态是已支付的状态，则需要退款
        if(orders.getPayStatus().equals(Orders.PAID)){
            //退款
            log.info("退款成功");
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());

        orderMapper.update(orders);
    }


    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(orders.getPayStatus().equals(Orders.PAID)){
            log.info("退款成功");
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders == null && !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.COMPLETED);
        orderMapper.update(orders);
    }


}
