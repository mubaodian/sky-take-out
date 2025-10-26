package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自定义定时任务类
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单的方法
     */
    @Scheduled(cron = "0 * * * * ?")//每分钟触发一次
    //@Scheduled(cron = "0/5 * * * * ?")
    public void processTimeoutOrder(){
        log.info("定时处理超时订单：{}",LocalDateTime.now());
        //查出有哪些订单超时了(订单状态为待付款，下单时间与现在时间相比超过15分钟)
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.getByStatusAndTimeLT(Orders.PENDING_PAYMENT,time);

        //更改超时订单的状态和信息
        if(ordersList.size()>0 && ordersList != null){
            for(Orders order : ordersList){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")//每天凌晨触发一次
    //@Scheduled(cron = "1/5 * * * * ?")
    public void processDliveryOrder(){
        log.info("定时处理货已送达 但还是在派送中的订单：{}",LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);//此时应该是隔天的凌晨,查询昨天还在派送中的订单
        List<Orders> ordersList = orderMapper.getByStatusAndTimeLT(Orders.DELIVERY_IN_PROGRESS, time);

        //更改超时订单的状态和信息
        if(ordersList.size()>0 && ordersList != null){
            for(Orders order : ordersList){
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
