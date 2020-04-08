package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jiahao
 * @create 2020-03-28 20:25
 */

@RestController
@RequestMapping("api/order")
public class OrderApiController {
    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
     private OrderService orderService;
    @Autowired
    private ProductFeignClient productFeignClient;

    @GetMapping("auth/trade")
    public Result<Map<String,Object>> trade(HttpServletRequest request){
        //必须先获取到用户id，根据用户id 获取到收货地址的列表
        String userId = AuthContextHolder.getUserId(request);
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        //送货清单 本质：订单明细
        //根据用户id  查询购物车中被选中的商品
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        //声明一个集合来存储订单详细数据
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
        for (CartInfo cartInfo : cartCheckedList) {
            //创建一个订单明细对象
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());

            // 添加到集合
            detailArrayList.add(orderDetail);
        }
        // 计算总金额
        OrderInfo orderInfo = new OrderInfo();
        //给订单明细赋值
        orderInfo.setOrderDetailList(detailArrayList);
        //计算总金额
        orderInfo.sumTotalAmount();
        //用map将数据存储起来
        Map<String, Object> map = new HashMap<>();
        //订单收货地址列表
        map.put("userAddressList", userAddressList);
        //订单明细集合
        map.put("detailArrayList", detailArrayList);
        // 保存件数
        map.put("totalNum", detailArrayList.size());
        //总金额
        map.put("totalAmount", orderInfo.getTotalAmount());
        //获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        map.put("tradeNo", tradeNo);



        return Result.ok(map);
    }
    //提交订单
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        // 获取到用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        //判断用户是否回退没有刷新页面提交订单
        //获取用户提交过来的流水号
        String tradeNo = request.getParameter("tradeNo");
        //比较流水号
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag){
            return Result.fail().message("不能重复提交订单！");
        }
        //  删除流水号
        orderService.deleteTradeNo(userId);
        //验证库存 获取订单明细集合
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //返回true 表示有库存 返回false 表示没有库存
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result){
                return Result.fail().message(orderDetail.getSkuName()+"库存不足");
            }
            //获取到商品的实时价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
            if (skuPrice.compareTo(orderDetail.getOrderPrice())!=0){
                //价格有变动
                //跟新一下商品的最新价格
                cartFeignClient.loadCartCache(userId);
                return Result.fail().message(orderDetail.getSkuName()+"价格有变动");

            }
        }


        // 验证通过，保存订单！
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }
    /**
     * 内部调用获取订单
     * @param orderId
     * @return
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable(value = "orderId") Long orderId){
        return orderService.getOrderInfo(orderId);
    }

    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){
        //获取对应数据
        String orderId = request.getParameter("orderId");
        //获取仓库Id与商品Id的对照关系
        String wareSkuMap = request.getParameter("wareSkuMap");
        // 拆单：获取到的子订单集合
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId),wareSkuMap);

        //声明一个集合存储Map
        ArrayList<Map> mapList = new ArrayList<>();

        //声明一个集合来存储orderInfo，先map、在转化成json字符串
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderService.initWareOrder(orderInfo);
            mapList.add(map);

        }
        //转化集合为字符串
        return JSON.toJSONString(mapList);

    }
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo) {
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }



}
