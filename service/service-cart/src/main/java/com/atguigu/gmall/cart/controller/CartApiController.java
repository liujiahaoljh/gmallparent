package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author jiahao
 * @create 2020-03-27 16:13
 */

@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    /**
     * 添加购物车
     *
     * @param skuId
     * @param skuNum
     * @param request
     * @return
     */
    @PostMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable("skuId") Long skuId,
                            @PathVariable("skuNum") Integer skuNum,
                            HttpServletRequest request) {
        // 如何获取userId  从网关的header中获取
        //如果能获取到用户id，说明我们已经登录了
        String userId = AuthContextHolder.getUserId(request);
        //未登录的时候 也能添加到购物车
        if (StringUtils.isEmpty(userId)) {
            // 获取临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.addToCart(skuId, userId, skuNum);
        return Result.ok();
    }

    //查询购物车列表控制器
    /**
     * 查询购物车
     *
     * @param request
     * @return
     */
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取临时用户Id
        String userTempId = AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartInfoList = cartService.getCartList(userId, userTempId);
        return Result.ok(cartInfoList);
    }
    //更改选中的状态
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            //获取用户的临时id
            userId = AuthContextHolder.getUserTempId(request);
        }
        // 调用更新方法
        cartService.checkCart(userId, isChecked, skuId);
        return Result.ok();

    }

    //删除购物车
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        // 获取userId
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            // 获取临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCart(skuId, userId);
        return Result.ok();
    }
    //根据用户id，获取到被选中的购物车商品
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable(value = "userId") String userId) {
        return cartService.getCartCheckedList(userId);
    }
    /**
     *
     * @param userId
     * @return
     */
    @GetMapping("loadCartCache/{userId}")
    public Result loadCartCache(@PathVariable("userId") String userId) {
        cartService.loadCartCache(userId);
        return Result.ok();
    }

}