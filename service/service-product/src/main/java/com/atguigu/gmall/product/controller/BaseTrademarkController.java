package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author jiahao
 * @create 2020-03-14 20:20
 */
@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    @ApiOperation(value = "分页列表")
    @GetMapping("{page}/{limit}")
    public Result index(
                        @ApiParam(name = "page", value = "当前页码", required = true) @PathVariable Long page,
                        @ApiParam(name = "limit", value = "每页记录数", required = true) @PathVariable Long limit) {

        Page<BaseTrademark> pageParam = new Page<>(page, limit);
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.selectPage(pageParam);
        return Result.ok(baseTrademarkIPage);
    }

    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id){
        // getById
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }
    //新增
    @PostMapping("save")
    //点击保存 save
    public Result save(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.save(baseTrademark);
        return Result.ok();

    }
    //删除
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    //修改
    @PutMapping("update")
    public Result update(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    @GetMapping("getTrademarkList")
    public Result getTrademarkList(){
        List<BaseTrademark> baseTrademarkList = baseTrademarkService.getBaseTrademarkList();
        return Result.ok(baseTrademarkList);
    }
}
