package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author jiahao
 * @create 2020-03-16 19:55
 */
@Api(tags = "商品SKU接口")
@RestController
@RequestMapping("admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    /**
     * 根据spuId 查询spuImageList
     * @param spuId
     * @return
     */
    @GetMapping("spuImageList/{spuId}")
    public Result<List<SpuImage>> getSpuImageList(@PathVariable Long spuId) {
        return Result.ok(manageService.getSpuImageList(spuId));
    }

    @GetMapping("spuSaleAttrList/{spuId}")
    public Result getSpuSaleAttrList(@PathVariable Long spuId){
        return Result.ok(manageService.getSpuSaleAttrList(spuId));
    }

    @GetMapping("/list/{page}/{limit}")
    public Result getSkuInfoPage(
            @ApiParam(name = "page", value = "当前页码", required = true)
            @PathVariable Long page,

            @ApiParam(name = "limit", value = "每页记录数", required = true)
            @PathVariable Long limit) {

        Page<SkuInfo> pageParam = new Page<>(page, limit);
        IPage<SkuInfo> skuInfoIPage = manageService.selectPage(pageParam);
        return Result.ok(skuInfoIPage);
    }

    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId) {
        manageService.onSale(skuId);
        return Result.ok();
    }

    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId) {
        manageService.canceSale(skuId);
        return Result.ok();
    }

}