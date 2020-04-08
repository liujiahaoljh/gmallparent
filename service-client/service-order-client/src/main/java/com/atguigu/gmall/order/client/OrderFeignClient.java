package com.atguigu.gmall.order.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

/**
 * @author jiahao
 * @create 2020-03-28 20:46
 */
@FeignClient(value = "service-order", fallback = OrderDegradeFeignClient.class)
public interface OrderFeignClient {

    @GetMapping("/api/order/auth/trade")
    Result<Map<String, Object>> trade();

    /**
     * 获取订单
     * @param orderId
     * @return
     */
    @GetMapping("/api/order/inner/getOrderInfo/{orderId}")
    OrderInfo getOrderInfo(@PathVariable(value = "orderId") Long orderId);

    /**
     * 秒杀下订单
     * @param orderInfo
     * @return
     */

    @PostMapping("/api/order/inner/seckill/submitOrder")
    Long submitOrder(OrderInfo orderInfo);
}
