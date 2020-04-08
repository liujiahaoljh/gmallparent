package com.atguigu.gmall.product.service;


import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


/**
 * @author jiahao
 * @create 2020-03-14 19:50
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {

    //做分页查询  上来就查询所有 不需要实体类接收任何的查询条件
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam);

    //查询所有数据
    List<BaseTrademark> getBaseTrademarkList();

}
