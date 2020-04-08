package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author jiahao
 * @create 2020-03-27 13:42
 */
public interface CartService {

    // 添加购物车 用户Id，商品Id，商品数量。
    void addToCart(Long skuId, String userId, Integer skuNum);


    /**
     * 通过用户Id 查询购物车列表
     * @param userId  用户真正登录时的用户Id
     * @param userTempId 临时的id
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 更新选中状态
     *
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, Integer isChecked, Long skuId);

    /**
     * 删除购物车
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 根据用户Id 查询购物车列表
     *
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    List<CartInfo> loadCartCache(String userId);
}
