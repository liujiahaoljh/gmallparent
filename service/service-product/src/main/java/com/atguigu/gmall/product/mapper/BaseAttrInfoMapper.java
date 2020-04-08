package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author jiahao
 * @create 2020-03-13 15:00
 */
//平台属性mapper
@Mapper
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {
    /**
     * 通过分类id，查询平台属性集合对象
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> selectBaseAttrInfoList(@Param("category1Id") Long category1Id, @Param("category2Id") Long category2Id,@Param("category3Id") Long category3Id);

    /**
     * 根据skuid查询平台属性 平台属性值
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> selectBaseAttrInfoListBySkuId(Long skuId);
}