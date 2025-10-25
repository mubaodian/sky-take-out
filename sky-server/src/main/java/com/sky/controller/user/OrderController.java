package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("user/order")
@Slf4j
@Api(tags = "用户端订单相关接口")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO  orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        return Result.success(orderPaymentVO);
    }

    @GetMapping("historyOrders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> page(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("历史订单查询：{}", ordersPageQueryDTO);
        PageResult pageResult = orderService.page(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO>  orderDetail(@PathVariable("id") Long id){
        log.info("查询订单id：{}",id);
        OrderVO orderVO = orderService.orderDetail(id);
        return Result.success(orderVO);
    }

    @PutMapping("cancel/{id}")
    @ApiOperation("用户取消订单")
    public Result cancel(@PathVariable Long id) throws Exception {
        log.info("取消订单：{}", id);
        orderService.cancel(id);
        return Result.success();
    }

    @PostMapping("repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable("id") Long id){
        log.info("再来一单：{}",id);
        orderService.repetition(id);
        return Result.success();
    }



}
