package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author jiahao
 * @create 2020-03-18 19:50
 */
@FeignClient(value = "service-item", fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {
    //把service-item中的方法暴露出来
    //根据skuId查询数据
    @GetMapping("/api/item/{skuId}")
    Result getItem(@PathVariable("skuId") Long skuId);
}
