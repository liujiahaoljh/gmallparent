package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author jiahao
 * @create 2020-03-13 16:02
 */
@Service //spring 注解
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private RabbitService rabbitService;

    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        QueryWrapper<BaseCategory2> wrapper = new QueryWrapper<>();
        //第一个参数，实体类的属性名，数据库表中对应的字段名称
        wrapper.eq("category1_id", category1Id);
        List<BaseCategory2> category2List = baseCategory2Mapper.selectList(wrapper);
        return category2List;
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {

        QueryWrapper<BaseCategory3> wrapper = new QueryWrapper<>();
        //第一个参数，实体类的属性名，数据库表中对应的字段名称
        wrapper.eq("category2_id", category2Id);
        List<BaseCategory3> category3List = baseCategory3Mapper.selectList(wrapper);
        return category3List;
    }

    //通过分类id  查询数据
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        //自定义mapper.xml，通过mybatis动态标签去查询

        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    @Override
    @Transactional//开启事务
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //什么时候是修改，什么时候是添加
        if (baseAttrInfo.getId() != null) {
            //此时是修改的操作
            baseAttrInfoMapper.updateById(baseAttrInfo);
        } else {
            //两张表 base_attr_info,base_attr-value
            //新增
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        //在修改的时候，先删除平台属性值，在新增
        QueryWrapper<BaseAttrValue> wrapper = new QueryWrapper<>();
        wrapper.eq("attr_id", baseAttrInfo.getId());
        baseAttrValueMapper.delete(wrapper);


        //两张表 base_attr_info,base_attr-value

        //平台属性值的添加
        //先获取平台属性值集合数据
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //循环遍历
        if (attrValueList.size() > 0 && attrValueList != null) {
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //细节 给当前的平台属性值对象添加attr_id 应该是baseAttrInfo.id
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        //给attrValueList赋值  平台属性集合
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));

        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo) {
        QueryWrapper<SpuInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id", spuInfo.getCategory3Id());
        wrapper.orderByDesc("id");
        /*
            第一个参数：Page 当前页，每页显示的条数
            第二个参数：查询条件是什么
         */
        return spuInfoMapper.selectPage(pageParam, wrapper);
    }

    //查询所有的销售属性数据
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    //保存spuInfo
    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {

        /*
            四张表的插入
            spuInfo
            spuImage
            spuSaleAttr
            spuSaleAttrValue

         */
        spuInfoMapper.insert(spuInfo);
        //图片
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }

        //销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);
                //获取销售属性值得集合数据
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());

                        //获取销售属性值中的name属性
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }


    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> wrapper = new QueryWrapper<>();
        wrapper.eq("spu_id", spuId);

        return spuImageMapper.selectList(wrapper);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        //一个是销售属性，一个是销售属性值 在两张表中
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        /**
         * skuInfo
         * skuAttrValue
         * skuSaleAttrValue
         * skuImage
         */
        skuInfoMapper.insert(skuInfo);
        //skuAttrValue 商品与平台属性关系
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size() > 0) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }
        //skuSaleAttrValue  商品与销售属性关系
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        // 调用判断集合方法
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
        //skuImage  商品的图片列表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
        //发送消息实现商品的上架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuInfo.getId());


    }

    //查询所有的sku信息
    @Override
    public IPage<SkuInfo> selectPage(Page<SkuInfo> pageParam) {
        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");

        return skuInfoMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public void onSale(Long skuId) {
        //商品上架
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);
        //发送消息实现商品的上架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuInfo.getId());


    }

    @Override
    public void canceSale(Long skuId) {
        //商品下架
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);
        //发送消息实现商品的下架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuInfo.getId());

    }


    @Override
    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX)
    public SkuInfo getSkuInfo(Long skuId) {

        //return getSkuInfoRedisson(skuId);
        return getSkuInfoDB(skuId);
    }

    //使用redis 做分布式锁
    private SkuInfo getSkuInfoRedisson(Long skuId) {
        //使用框架redisson解决分布式锁

        SkuInfo skuInfo = null;
        try {
            //定义key sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //获取里面的值  redis5种数据类型
            //获取缓存数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //如果从缓存里获取的数据是空
            if (skuInfo == null) {
                //使用redisson
                //定义锁的key
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(lockKey);
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (res) {
                    try {
                        //处理业务逻辑 获取数据库的数据
                        skuInfo = getSkuInfoDB(skuId);
                        if (skuInfo == null) {
                            // 为了避免缓存穿透 应该给空的对象放入缓存
                            SkuInfo skuInfo1 = new SkuInfo(); //对象的地址
                            redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        // 查询数据库的时候，有值
                        redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);

                        return skuInfo;

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        //解锁
                        lock.unlock();
                    }

                } else {
                    //其他线程等待
                    Thread.sleep(1000);
                    return skuInfo;
                }
            } else {

                // 如果用户查询的数据在数据库中根本不存在的时候第一次会将一个空对象直接放入缓存。
                // 那么第二次查询的时候，缓存中有一个空对象 防止缓存穿透
                if (null == skuInfo.getId()) {
                    return null;
                }
                //缓存数据不为空
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //为了防止缓存宕机 从数据库中获取数据
        return getSkuInfoDB(skuId);


    }

    //使用redis 做分布式锁
    private SkuInfo getSkuInfoRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //定义key sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //获取里面的值  redis5种数据类型
        /*
            redis4.0以前：
            String：通常是常量（验证码）
            set：通常获取两个结果集（交集，补集，差集）
            Hash：通常是存储对象（属性=value）
            List：通常是存储队列（秒杀）
            zest：通常是排序，排名
            redis5.0以后：
                多一种stream
         */
            //获取缓存数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //如果从缓存里获取的数据是空
            if (skuInfo == null) {
                //定义锁的key
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                //定义锁的值
                String uuid = UUID.randomUUID().toString().replace("-", "");
                //上锁
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (isExist) {
                    //执行成功了  上锁了
                    System.out.println("获取到分布式锁");
                    //真正的去获取数据库的数据
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo == null) {
                        // 为了避免缓存穿透 应该给空的对象放入缓存
                        SkuInfo skuInfo1 = new SkuInfo(); //对象的地址
                        redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    // 查询数据库的时候，有值
                    redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    // 解锁：使用lua 脚本解锁
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    // 设置lua脚本返回的数据类型
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    // 设置lua脚本返回类型为Long
                    redisScript.setResultType(Long.class);
                    redisScript.setScriptText(script);
                    // 删除key 所对应的 value
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);
                    return skuInfo;
                } else {
                    //其他线程等待
                    Thread.sleep(1000);
                    return skuInfo;
                }
            } else {

                // 如果用户查询的数据在数据库中根本不存在的时候第一次会将一个空对象直接放入缓存。
                // 那么第二次查询的时候，缓存中有一个空对象 防止缓存穿透
                if (null == skuInfo.getId()) {
                    return null;
                }
                //缓存数据不为空
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //为了防止缓存宕机 从数据库中获取数据
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoDB(Long skuId) {
        //一下代码都是来自于数据库
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
            QueryWrapper<SkuImage> wrapper = new QueryWrapper<>();
            wrapper.eq("sku_id", skuId);
            List<SkuImage> skuImages = skuImageMapper.selectList(wrapper);
            skuInfo.setSkuImageList(skuImages);
        }
        return skuInfo;
    }

    //根据三级分类id查询分类信息
    @Override
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {

        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
            return skuInfo.getPrice();
        }
        return new BigDecimal(0);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        //声明一个map来存储数据
        HashMap<Object, Object> hashMap = new HashMap<>();

        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        if (mapList != null && mapList.size() > 0) {
            for (Map map : mapList) {
//                String key = (String) map.get("value_ids");
//                String value = (String) map.get("sku_id");
                hashMap.put(map.get("value_ids"), map.get("sku_id"));
            }
        }
        return hashMap;
    }

    @Override
    @GmallCache(prefix = "category")
    public List<JSONObject> getBaseCategoryList() {
        //声明json集合对象
        List<JSONObject> list = new ArrayList<>();

        //1.先查询所有的分类数据
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //按照一级分类id 进行分组
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //开始准备构建
        int index = 1;
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            //初始化一个一级分类对象
            //获取一级分类id
            Long category1Id = entry1.getKey();
            //获取一级分类下的所有集合数据
            List<BaseCategoryView> category2List1 = entry1.getValue();
            JSONObject category1 = new JSONObject();
            //赋值
            category1.put("index", index);
            category1.put("categoryId", category1Id);
            // 一级分类名称
            category1.put("categoryName", category2List1.get(0).getCategory1Name());
            // 变量迭代
            index++;

        // 循环获取二级分类数据
        Map<Long, List<BaseCategoryView>> category2Map = category2List1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
        // 声明二级分类对象集合
        List<JSONObject> category2Child = new ArrayList<>();
        // 循环遍历
        for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
            // 获取二级分类Id
            Long category2Id = entry2.getKey();
            // 获取二级分类下的所有集合
            List<BaseCategoryView> category3List1 = entry2.getValue();
            // 声明二级分类对象
            JSONObject category2 = new JSONObject();

            category2.put("categoryId", category2Id);
            category2.put("categoryName", category3List1.get(0).getCategory2Name());
            category2Child.add(category2);

            //处理三级分类数据
            //声明一个三级分类对象集合
            List<JSONObject> category3Child = new ArrayList<>();
            // 循环三级分类数据
            category3List1.stream().forEach(category3View -> {
                //声明一个三级分类对象
                JSONObject category3 = new JSONObject();
                category3.put("categoryId",category3View.getCategory3Id());
                category3.put("categoryName",category3View.getCategory3Name());
                //将每一个三级分类对象添加到集合
                category3Child.add(category3);
            });

            // 将三级数据放入二级里面
            category2.put("categoryChild",category3Child);

        }
            // 将二级数据放入一级里面
            category1.put("categoryChild",category2Child);
            list.add(category1);
        }
        return list;
    }

    //根据tmId查询品牌对象
    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }

    //根据商品id查询平台属性 平台属性值
    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);
    }

    //通过attrId获取数据
        private List<BaseAttrValue> getAttrValueList (Long attrId){

            QueryWrapper wrapper = new QueryWrapper<BaseAttrValue>();
            wrapper.eq("attr_id", attrId);
            List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.selectList(wrapper);
            return baseAttrValueList;
        }
    }
