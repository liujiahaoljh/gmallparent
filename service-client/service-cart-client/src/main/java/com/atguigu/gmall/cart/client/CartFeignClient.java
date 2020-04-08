package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.cart.client.impl.CartDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * @author jiahao
 * @create 2020-03-28 13:44
 */
@FeignClient(name = "service-cart",fallback = CartDegradeFeignClient.class)
public interface CartFeignClient {

    //远程调用的接口
    @PostMapping("/api/cart/addToCart/{skuId}/{skuNum}")
    Result addToCart(@PathVariable("skuId") Long skuId, @PathVariable("skuNum") Integer skuNum);

    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    List<CartInfo> getCartCheckedList(@PathVariable("userId") String userId);
    // 加载数据
    @GetMapping("/api/cart/loadCartCache/{userId}")
    Result loadCartCache(@PathVariable("userId") String userId);
}
