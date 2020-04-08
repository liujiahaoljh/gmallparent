package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author jiahao
 * @create 2020-03-13 15:04
 */
public interface ManageService {

    /**
     * 查询所有的一级分类信息
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类Id 查询二级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类Id 查询三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类id获取平台属性
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 保存平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     *  通过attrId获取平台属性id
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(Long attrId);

    /**
     * spu分页查询
     * @param pageParam
     * @param spuInfo
     * @return
     */
    IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo);

    /**
     * 查询所有的销售属性数据
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存spuInfo
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId查询商品的图片列表
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 通过spuId 查询销售属性数据
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    /**
     * 保存skuInfo的方法
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 当前页  每页显示的记录数等信息 page
     * 查询所有的skuInfo数据
     * @param pageParam
     * @return
     */
    IPage<SkuInfo> selectPage(Page<SkuInfo> pageParam);

    /**
     * 商品的上架
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 商品的下架
     * @param skuId
     */
    void canceSale(Long skuId);

    /**
     * 根据商品id 查询skuInfo信息
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 通过三级分类id查询分类信息
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

    /**
     * 通过skuId 获取商品价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据skuId，spuId  查询销售属性 硬切锁定销售属性值
     *
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 通过spuId查询数据
     * @param spuId
     * @return
     */
    Map getSkuValueIdsMap(Long spuId);


    /**
     * 获取全部分类信息
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 通过品牌Id 来查询数据
     * @param tmId
     * @return
     */
    BaseTrademark getTrademarkByTmId(Long tmId);

    /**
     * 通过skuId 集合来查询数据
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);
}
