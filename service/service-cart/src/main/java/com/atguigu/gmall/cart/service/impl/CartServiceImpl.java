package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author jiahao
 * @create 2020-03-27 13:46
 */
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /**
         * 1.如果是第一次购买商品，直接添加到购物车
         * 2.如果不是第一次购买，购物车数量加一
         * 3.将数据保存到数据库，同时放入缓存
         */

        // 需要更新缓存
        // 确定用什么数据类型来存储购物车 Hash 数据结构：
        // hset(key,field,value) key=user:userId:cart field=skuId value=cartInfo.toString()
        // 获取cartKey
        String cartKey = getCartKey(userId);
        if (!redisTemplate.hasKey(cartKey)){
            // 加载数据库的数据到缓存
            loadCartCache(userId);
        }

        // 查询购物车是否有该商品 需要以用户Id为基准，还需要以商品Id为基准。
        // select * from cart_info where user_id = ? sku_id = ? || 或者登录 selectOne
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id",skuId).eq("user_id",userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(wrapper);
        // 说明购物车中有该商品
        if (cartInfoExist!=null){
            // 商品数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            // 因为skuPrice 在数据库中不存在，它又表示实时价格，所以需要查询一下。
            // 实时价格 = skuInfo.price
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            cartInfoExist.setSkuPrice(skuPrice);
            // 放入数据库
            cartInfoMapper.updateById(cartInfoExist);
            // 需要更新缓存
        } else {
            // 购物车没有该商品
            // 获取当前的商品信息
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            // 将当前商品的信息赋值给cartInfo
            CartInfo cartInfo = new CartInfo();

            cartInfo.setSkuPrice(skuInfo.getPrice()); // 开始添加购物车时，实时价格就是商品价格
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setUserId(userId);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuName(skuInfo.getSkuName());
            // 放入数据库
            cartInfoMapper.insert(cartInfo);

            // 需要更新缓存
            cartInfoExist = cartInfo;
        }


        // 向缓存中放入数据
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);

        // 还需要设置一个缓存中购物车的过期时间
        setCartKeyExpire(cartKey);

    }

    //通过用户Id 查询购物车列表
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        //无论是哪个用户id 都需要先查询缓存
        List<CartInfo> cartInfoList = new ArrayList<>();
        //用户未登录
        if (StringUtils.isEmpty(userId)){
            //获取未登录购物车的集合数据
            cartInfoList = getCartList(userTempId);
        }
        // 用户登录情况
        if (!StringUtils.isEmpty(userId)){
            // 登录的时候合并购物车：
            // 获取未登录购物车数据
            List<CartInfo> cartList = getCartList(userTempId);
            // 未登录购物车数据不是空的
            if (!CollectionUtils.isEmpty(cartList)){
                // 此时开始合并购物车
                // 第一个参数未登录购物车数据，第二个参数是用户Id{可以通过用户Id 查询登录数据}
                cartInfoList = mergeToCartList(cartList,userId);
                // 删除未登录购物车数据
                deleteCartList(userTempId);
            }
            // 如果未登录购物车集合中的数据是空的，或者是说根本没有临时用户id
            if (CollectionUtils.isEmpty(cartList) || StringUtils.isEmpty(userTempId)){
                cartInfoList = getCartList(userId);
            }
        }
        // return cartInfoList;
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        //修改数据库
        //第一个参数cartInfo 表示修改的内容，第二个参数表示根据什么条件去修改
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId).eq("sku_id",skuId);
        cartInfoMapper.update(cartInfo,wrapper);

        //修改缓存
        //必须先获取缓存的key
        String cartKey = getCartKey(userId);
        //获取缓存中的数据
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        if (boundHashOperations.hasKey(skuId.toString())){
            //获取缓存中的数据
            CartInfo cartInfoUpd = (CartInfo) boundHashOperations.get(skuId.toString());
            // 修改数据
            cartInfoUpd.setIsChecked(isChecked);
            // 还需要将修改之后的对象再放入缓存
            boundHashOperations.put(skuId.toString(),cartInfoUpd);
            // 修改了缓存，那么我们就需要再次给一个过期时间
            setCartKeyExpire(cartKey);
        }




    }

    //删除购物车
    @Override
    public void deleteCart(Long skuId, String userId) {

        //删除数据库
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId).eq("sku_id",skuId);
        cartInfoMapper.delete(wrapper);
        //删除缓存
        //先获取到购物车的key
        String cartKey = getCartKey(userId);
        BoundHashOperations<String, String, CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        if (hashOperations.hasKey(skuId.toString())){
            //删除购物车中所对应的商品
            hashOperations.delete(skuId.toString());
        }


    }

    //根据用户Id 查询购物车列表
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //1.查询购物车中选中的商品
        List<CartInfo> cartInfoList = new ArrayList<>();
        //去缓存查找
        String cartKey = getCartKey(userId);
        List<CartInfo> cartInfoCheckedList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoCheckedList)){
            for (CartInfo cartInfo : cartInfoCheckedList) {
                //选中的商品
                if (cartInfo.getIsChecked().intValue()==1){
                    cartInfoList.add(cartInfo);
                }
            }
        }
        return cartInfoList;
    }

    //删除购物车数据
    private void deleteCartList(String userTempId) {
        // 删除购物车 数据存在 mysql + redis
        // delete from cart_info where user_id = ?
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userTempId);
        cartInfoMapper.delete(wrapper);

        // 删除缓存
        // 获取到购物车key
        String cartKey = getCartKey(userTempId);
        // 判断cartKey 在缓存是否存在
        if (redisTemplate.hasKey(cartKey)){
            // 删除数据即可！
            redisTemplate.delete(cartKey);
        }


    }

    //合并购物车
    private List<CartInfo> mergeToCartList(List<CartInfo> cartList, String userId) {
    // 1.获取到用户登录购物车数据
    List<CartInfo> cartListLogin = getCartList(userId);
    // 将cartListLogin 登录的用户数据集合转换成map集合map 中的key = skuId,value=cartInfo
    Map<Long, CartInfo> cartInfoMapLogin = cartListLogin.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
    // 需要根据商品Id 进行判断
    // 循环未登录的购物车
        for (CartInfo cartInfoNoLogin : cartList) {
        // 获取未登录购物车中的商品Id
        Long skuId = cartInfoNoLogin.getSkuId();
        // 判断登录中的购物车数据是否有相同的商品Id
        if (cartInfoMapLogin.containsKey(skuId)){ // 说明登录的购物车中有未登录购物车的数据
            // 开始做数量相加 获取登录的购物车对象
            CartInfo cartInfoLogin = cartInfoMapLogin.get(skuId);
            cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());
            // 合并的时候，需要注意购物车选中状态！
            // 登录的时候，有选中，未选中，未登录也可以有选中，未选中！
            // 如果未登录购物车中的商品被选中了，那么我们需要将数据库中的状态改为选中
            if (cartInfoNoLogin.getIsChecked().intValue()==1){
                cartInfoLogin.setIsChecked(1);
            }
            // 更新数据库
            cartInfoMapper.updateById(cartInfoLogin);
        } else {
            //  未登录购物车与登录购物车中的商品id 不一致。 未登录有这个商品，登录没有这个商品
            //  这种情况下应该直接添加到数据 未登录的用户Id 是临时的。应该将用户Id 变成登录的用户Id
            //  cartInfoNoLogin.setId(null); // 为了确保在插入数据的时候，能够让id 自动增长！
            cartInfoNoLogin.setUserId(userId);
            // 插入数据库
            cartInfoMapper.insert(cartInfoNoLogin);
        }
    }
    // 数据汇总：
    List<CartInfo> cartInfoList = loadCartCache(userId);

        return cartInfoList;
}


    //根据用户id（登录未登录） 获取购物车集合
    private List<CartInfo> getCartList(String userId) {
        //声明一个集合
        List<CartInfo> cartInfoList = new ArrayList<>();
        //根据用户id 获取缓存数据
        if (StringUtils.isEmpty(userId)) return cartInfoList;
        //购物车数据应该先查询缓存
        String cartKey = getCartKey(userId);
        //根据cartKey 获取缓存数据
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            // 购物车列表显示有顺序：按照商品的更新时间 降序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    // 比较方法
                    return o1.getId().toString().compareTo(o2.getId().toString());
                }
            });
            //将缓存中去到的数据排序并且返回
            return cartInfoList;
        } else {
            // 缓存中没用数据！  从数据库获取数据 并且加载到缓存
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }

    }

    //表示根据用户id查询数据库 并且将数据放入缓存
    public List<CartInfo> loadCartCache(String userId) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(wrapper);
        //根据业务需求将数据放入缓存
        if (CollectionUtils.isEmpty(cartInfoList)) {
            return cartInfoList;
        }
        //获取购物车的key
        String cartKey = getCartKey(userId);
        HashMap<String, CartInfo> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            //查询最新的价格给当前的对象
            cartInfo.setSkuPrice( productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            //将数据放入map
            map.put(cartInfo.getSkuId().toString(), cartInfo);
        }

        redisTemplate.opsForHash().putAll(cartKey, map);
        // 设置过期时间
        this.setCartKeyExpire(cartKey);
        //从数据库中查询数据并且返回
        return cartInfoList;
    }

    // 设置过期时间
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    //获取购物车的key
    private String getCartKey(String userId) {
        //定义key user:userId:cart
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }

}
