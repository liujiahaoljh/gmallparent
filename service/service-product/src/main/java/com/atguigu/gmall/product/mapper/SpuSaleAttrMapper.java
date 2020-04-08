package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author jiahao
 * @create 2020-03-16 13:29
 */
@Mapper
public interface SpuSaleAttrMapper extends BaseMapper<SpuSaleAttr> {

    //根据spuId 查询数据
    List<SpuSaleAttr> selectSpuSaleAttrList(Long spuId);

    //根据skuId，spuId  查询销售属性 硬切锁定销售属性值
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(@Param(value = "skuId") Long skuId, @Param("spuId") Long spuId);
}