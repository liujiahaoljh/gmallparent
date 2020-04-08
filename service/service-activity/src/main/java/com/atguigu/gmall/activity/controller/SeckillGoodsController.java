package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author jiahao
 * @create 2020-04-06 16:53
 */
@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsController {
    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("/findAll")
    public Result findAll() {

        List<SeckillGoods> list = seckillGoodsService.findAll();
        return Result.ok(list);
    }

    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId) {
        return Result.ok(seckillGoodsService.getSeckillGoods(skuId));
    }

    /**
     * 获取下单码
     *
     * @param skuId
     * @return
     */
    @GetMapping("auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        //获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //通过skuId 获取当前要秒杀的商品
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoods(skuId);
        if (seckillGoods != null) {
            //判断当前的商品是否能秒杀
            //系统时间
            Date curTime = new Date();
            if (DateUtil.dateCompare(seckillGoods.getStartTime(), curTime) && DateUtil.dateCompare(curTime, seckillGoods.getEndTime())) {
                //可以动态生成，放在redis缓存
                String skuIdStr = MD5.encrypt(userId);
                return Result.ok(skuIdStr);

            }
        }
        return Result.fail().message("获取下单码失败");
    }

    @PostMapping("auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable("skuId") Long skuId, HttpServletRequest request) throws Exception {
        //校验下单码 下单码是由MD5加密得到的
        //先获取userId
        String userId = AuthContextHolder.getUserId(request);
        //传递过来的下单码
        String skuIdStr = request.getParameter("skuIdStr");
        //判断 防止用户通过浏览器的地址直接修改下单码
        if (skuIdStr.equals(MD5.encrypt(userId))){
            //请求不合法，传递过来的下单码是错误的
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //判断缓存的标识位
        //获取状态位
        String state = (String) CacheHelper.get(skuId.toString());
        if (!skuIdStr.equals(MD5.encrypt(userId))) {
            //请求不合法
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //表示当前商品可以秒杀
        if ("1".equals(state)){
            //存储用户信息  只存储skuId ，用户Id
            UserRecode userRecode = new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(skuId);
            //发送消息将获取到秒杀资格的用户存储起来
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER, MqConst.ROUTING_SECKILL_USER, userRecode);
        }else {
            //已售罄
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }
        return Result.ok();
    }
    /**
     * 查询秒杀状态
     * @return
     */
    @GetMapping(value = "auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        //当前登录用户
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.checkOrder(skuId, userId);
    }
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request) {
        // 获取到用户Id
        String userId = AuthContextHolder.getUserId(request);

        // 先得到用户想要购买的商品！
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode) {
            return Result.fail().message("非法操作");
        }
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();

        //获取用户地址
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        // 声明一个集合来存储订单明细
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        orderDetail.setSkuNum(orderRecode.getNum());
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        // 添加到集合
        detailArrayList.add(orderDetail);

        // 计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();

        Map<String, Object> result = new HashMap<>();
        result.put("userAddressList", userAddressList);
        result.put("detailArrayList", detailArrayList);
        // 保存总金额
        result.put("totalAmount", orderInfo.getTotalAmount());
        return Result.ok(result);
    }
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //下单数据存在缓存中
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode) {
            return Result.fail().message("非法操作");
        }

        //如果没有用户id，将用户赋值，如果有了不需要赋值了
        orderInfo.setUserId(Long.parseLong(userId));

        Long orderId = orderFeignClient.submitOrder(orderInfo);
        if (null == orderId) {
            return Result.fail().message("下单失败，请重新操作");
        }

        //删除下单信息
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);
        //下单记录
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId, orderId.toString());

        return Result.ok(orderId);

    }

}



