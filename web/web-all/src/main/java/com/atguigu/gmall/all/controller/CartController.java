package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

/**
 * @author jiahao
 * @create 2020-03-28 13:59
 */
@Controller
public class CartController {
    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * 查看购物车
     * @param request
     * @return
     */
    @RequestMapping("cart.html")
    public String index(HttpServletRequest request){
        return "cart/index";
    }

    /**
     * 添加购物车
     * @param skuId
     * @param skuNum
     * @param request
     * @return
     */

    /*
        使用@RequestParam时，URL是这样的：http://host:port/path?参数名=参数值
        使用@PathVariable时，URL是这样的：http://host:port/path/参数值
     */
    @RequestMapping("addCart.html")
    public String addCart(@RequestParam(name = "skuId") Long skuId,
                          @RequestParam(name = "skuNum") Integer skuNum,
                          HttpServletRequest request){
        //这个控制器中我们无法直接拿到skuId
        //我们定义了一个拦截器在web-util中 获取文件信息放入到header中
        cartFeignClient.addToCart(skuId, skuNum);

        //需要保存的信息  skuInfo skuNum
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "cart/addCart";
    }
}
