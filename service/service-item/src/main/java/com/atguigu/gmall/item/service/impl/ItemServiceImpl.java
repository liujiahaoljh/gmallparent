package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.aspectj.weaver.ast.Var;
import org.bouncycastle.cert.ocsp.Req;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private ListFeignClient listFeignClient;
    @Override
    public Map<String, Object> getBySkuId(Long skuId) {

        // 构建返回map
        Map<String, Object> result = new HashMap<>();
        //重构代码 异步编排：
        //需要有返回值
        CompletableFuture<SkuInfo> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //获取skuInfo数据
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            result.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);
        // 查询所有的销售属性销售属性值回显数据
        CompletableFuture<Void> spuSaleAttrCompletableFuture  = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
            result.put("spuSaleAttrList", spuSaleAttrList);
        }, threadPoolExecutor);

        //获取相应的map 集合数据 点击销售属性切换的数据
        CompletableFuture<Void> skuValueIdsMapCompletableFuture  = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            // 将map 转化为json 数据
            String skuValueIdsMapJson = JSON.toJSONString(skuValueIdsMap);
            //保存json数据
            result.put("valuesSkuJson", skuValueIdsMapJson);
        }, threadPoolExecutor);

        //获取最新的商品价格
        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            result.put("price", skuPrice);
        }, threadPoolExecutor);

        // 通过三级分类Id 查询分类数据
        CompletableFuture<Void> categoryViewCompletableFuture  = skuCompletableFuture.thenAcceptAsync((skuInfo) -> {
            //获取分类数据
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            result.put("categoryView", categoryView);
        }, threadPoolExecutor);

        //利用异步编排调用商品热度的接口
        CompletableFuture<Void> hotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);


        //将所有线程进行一个汇总
        //allof

        CompletableFuture.allOf(skuCompletableFuture,
                spuSaleAttrCompletableFuture,
                skuValueIdsMapCompletableFuture,
                skuPriceCompletableFuture,
                categoryViewCompletableFuture,
                hotScoreCompletableFuture).join();


        //单线程获取商品详情：
//        // 通过feien 远程调用ProductApiController.getSkuInfo
//        // 通过商品Id 查询skuInfo
//        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
//        // 查询所有的销售属性销售属性值回显数据
//        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
//        // 查询销售属性值Id 拼接成的字符串
//        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//        // 将map 转化为json 数据
//        String skuValueIdsMapJson = JSON.toJSONString(skuValueIdsMap);
//        // 通过三级分类Id 查询分类数据
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
//        // 查询价格
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//
//        // 保存数据
//        result.put("skuInfo",skuInfo);
//        result.put("categoryView",categoryView);
//        result.put("price",skuPrice);
//        result.put("valuesSkuJson",skuValueIdsMapJson);
//        result.put("spuSaleAttrList",spuSaleAttrList);

        // map 返回
        return result;
    }

}
