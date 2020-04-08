package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

/**
 * @author jiahao
 * @create 2020-03-23 22:08
 */
public interface SearchService {
    //商品的上架  skuId
    void upperGoods(Long skuId);

    //商品的下架  skuId
    void lowerGoods(Long skuId);

    /**
     * 更新热点
     * @param skuId
     */
    void incrHotScore(Long skuId);

    //检索列表
    SearchResponseVo search(SearchParam searchParam) throws Exception;

}
