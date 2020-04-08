package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;

import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author jiahao
 * @create 2020-03-18 19:56
 */
@Controller
@RequestMapping
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;
    @RequestMapping("{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model){
        // 通过skuId 查询skuInfo
        Result<Map> result = itemFeignClient.getItem(skuId);
        model.addAllAttributes(result.getData());
        //保存数据
        return "item/index";
    }
}
