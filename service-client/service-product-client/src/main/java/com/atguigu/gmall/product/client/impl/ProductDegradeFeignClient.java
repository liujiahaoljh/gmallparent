package com.atguigu.gmall.product.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author jiahao
 * @create 2020-03-18 13:56
 *
 * 如果远程调用失败 则会到ProductDegradeFeignClient找实现方法
 * 想当于一种熔断保护措施
 */
@Component
public class ProductDegradeFeignClient implements ProductFeignClient {


    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        return null;
    }

    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        return null;
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        return null;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return null;
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(Long spuId) {
        return null;
    }

    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        return null;
    }

    @Override
    public Result getBaseCategoryList() {
        return null;
    }

    @Override
    public BaseTrademark getTrademark(Long tmId) {
        return null;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return null;
    }
}
