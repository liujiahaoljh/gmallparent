package com.atguigu.gmall.item.service;

/**
 * @author jiahao
 * @create 2020-03-17 19:40
 */

import java.util.Map;

/**
 * 汇总数据的接口
 */
public interface ItemService {
    //如何定义数据的接口呢  sku基本信息 sku的分类信息
    Map<String,Object> getBySkuId(Long skuId);
}
