package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sun.security.util.ManifestEntryVerifier;

import java.util.List;
import java.util.Locale;

/**
 * @author jiahao
 * @create 2020-03-13 20:04
 */
@RestController
@Api(tags = "商品基础属性接口")
@RequestMapping("admin/product")
public class BaseManageController {

    @Autowired
    private ManageService manageService;

    //查询一级分类数据
    @GetMapping("getCategory1")
    public Result<List<BaseCategory1>> getCategory1(){
        List<BaseCategory1> category1 = manageService.getCategory1();
        //返回的是数据还有code码，以及消息！
        return Result.ok(category1);
    }

    //根据一级分类id，查询二级分类数据
    @GetMapping("getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable("category1Id") Long category1Id) {
        List<BaseCategory2> baseCategory2List = manageService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }
    //根据二级分类id，查询三级分类数据
    @GetMapping("getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable("category2Id") Long category2Id) {
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    //根据分类Id 获取平台属性数据
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> attrInfoList(@PathVariable("category1Id") Long category1Id,
                                                   @PathVariable("category2Id") Long category2Id,
                                                   @PathVariable("category3Id") Long category3Id) {
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(baseAttrInfoList);
    }

    //现货区到页面数据 vue项目 通常会将一个json对象传递给后台
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }


    @GetMapping("getAttrValueList/{attrId}")
    public Result<List<BaseAttrValue>> getAttrValueList(@PathVariable("attrId") Long attrId) {
        //先查询平台属性，从平台属性下面获取响应的平台属性值
        //attrId是base_value.attr_id 也是baseAttrInfo.id
        //平台属性：平台属性值是1：n
        //应该先查询平台属性，在查询平台属性值
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        //获取到平台属性值的集合
        List<BaseAttrValue> baseAttrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(baseAttrValueList);
    }

    @GetMapping("{page}/{size}")
    public Result<IPage<SpuInfo>> index(@ApiParam(name = "page",value = "当前页码",required = true) @PathVariable Long page,
                                        @ApiParam(name = "size", value = "每页记录数", required = true) @PathVariable Long size,
                                        @ApiParam(name = "spuInfo", value = "查询对象", required = false) SpuInfo spuInfo){
        //封装分页查询数据
        Page<SpuInfo> pageParam  = new Page<>(page, size);

        IPage<SpuInfo> spuInfoIPage = manageService.selectPage(pageParam, spuInfo);

        return Result.ok(spuInfoIPage);

    }


}
