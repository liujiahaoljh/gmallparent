package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author jiahao
 * @create 2020-04-01 13:47
 */
@Controller
@RequestMapping
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    /**
     * 支付页
     * @param request
     * @return
     */
    @GetMapping("pay.html")
    public String success(HttpServletRequest request, Model model) {
        //获取订单的id
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        //后台应该存储一个orderInfo的对象
        model.addAttribute("orderInfo", orderInfo);
        return "payment/pay";
    }
    /**
     * 支付成功页
     * @param
     * @return
     */
    @GetMapping("pay/success.html")
    public String success() {
        return "payment/success";
    }

}