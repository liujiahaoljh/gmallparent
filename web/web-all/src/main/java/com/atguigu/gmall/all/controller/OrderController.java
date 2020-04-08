package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.apache.commons.lang3.builder.ToStringSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author jiahao
 * @create 2020-03-28 20:53
 */
@Controller
public class OrderController  {

    @Autowired
    private OrderFeignClient orderFeignClient;

    /**
     * 去结算按钮的处理
     * @param model
     * @param request
     * @return
     */
    @GetMapping("trade.html")
    public String trade(Model model, HttpServletRequest request){
        Result<Map<String, Object>> result = orderFeignClient.trade();

        model.addAllAttributes(result.getData());

        return "order/trade";
    }

}
