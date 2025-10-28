package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        //将日期装到list集合中
        List<LocalDate> dateList = getLocalDates(begin, end);
        //将集合转为字符串类型
        String dateListStr = StringUtils.join(dateList,",");

        //将每天的营业额装到一个集合中
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : dateList){
            //查询date日期对应的营养额数据，营业额是指：状态为“已完成”的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//这一天的零点
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        String turnoverListStr = StringUtils.join(turnoverList,",");

        //返回vo对象
        TurnoverReportVO t =  TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
        return t;
    }

    //将日期装到list集合中
    private static List<LocalDate> getLocalDates(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        //将日期装到list集合中
        List<LocalDate> dateList = getLocalDates(begin, end);
        //将集合转为字符串类型
        String dateListStr = StringUtils.join(dateList,",");


        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//这一天的零点
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            //查询每天用户新增量，并封装到集合中
            Map map1 = new HashMap();
            map1.put("begin",beginTime);
            map1.put("end",endTime);
            Integer newUserCount = userMapper.getUserCount(map1);
            newUserCount = newUserCount == null ? 0 : newUserCount;
            newUserList.add(newUserCount);

            //查询每天的总用户数量
            Map map2 = new HashMap();
            map2.put("end",endTime);
            Integer totalUserCount = userMapper.getUserCount(map2);
            totalUserCount = totalUserCount == null ? 0 : totalUserCount;
            totalUserList.add(totalUserCount);
        }

        String newUserListStr = StringUtils.join(newUserList,",");

        String totalUserListStr = StringUtils.join(totalUserList,",");

        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(dateListStr)
                .newUserList(newUserListStr)
                .totalUserList(totalUserListStr)
                .build();
        return userReportVO;
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {//将日期装到list集合中
        List<LocalDate> dateList = getLocalDates(begin, end);
        //将集合转为字符串类型
        String dateListStr = StringUtils.join(dateList,",");

        List<Integer> totalOrderList = new ArrayList<>();
        List<Integer> OrderList = new ArrayList<>();
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);

            //查询每天总订单数据
            Integer totalOrderCount = orderMapper.countByMap(map);
            totalOrderCount = totalOrderCount == null ? 0 : totalOrderCount;
            totalOrderList.add(totalOrderCount);
            //查询每天有效订单数，就是每天"已完成"的订单数
            map.put("status",Orders.COMPLETED);
            Integer orderCount = orderMapper.countByMap(map);
            orderCount = orderCount == null ? 0 : orderCount;
            OrderList.add(orderCount);
        }

        Integer totalOrderCount = totalOrderList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = OrderList.stream().reduce(Integer::sum).get();
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return new OrderReportVO().builder()
                .dateList(dateListStr)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCountList(StringUtils.join(totalOrderList,","))
                .validOrderCountList(StringUtils.join(OrderList,","))
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 查询销量排名top10接口
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO salesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop(beginTime, endTime);

        List<String> nameList = new ArrayList<>();
        List<String> numberList = new ArrayList<>();
        for(GoodsSalesDTO goodsSalesDTO : goodsSalesDTOList){
            nameList.add(goodsSalesDTO.getName());
            numberList.add(goodsSalesDTO.getNumber().toString());
        }
        return new SalesTop10ReportVO().builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
    }
}
