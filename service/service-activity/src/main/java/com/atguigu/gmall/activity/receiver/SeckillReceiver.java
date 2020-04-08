package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author jiahao
 * @create 2020-04-06 13:55
 */
@Component
public class SeckillReceiver {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importGoods(Message message, Channel channel) throws IOException {
        //什么样的商品算是秒杀的商品
        QueryWrapper<SeckillGoods> queryWrapper = new QueryWrapper<>();
        // 查询审核状态1 并且库存数量大于0，当天的商品
        queryWrapper.eq("status",1).gt("stock_count",0);
        queryWrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(queryWrapper);

        //当前集合就是被秒杀的商品集合数据
        //将商品集合数据放入缓存等一系列操作
        if (seckillGoodsList!=null && seckillGoodsList.size()>0){
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                //遍历出来放入缓存中  什么类型来存储数据
                //使用hset来存储 hset（key,field,value）
                //先判断缓存中是否有要秒杀哦的数据，如果有就不放入，没有则放入数据
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                if (flag){
                    continue;
                }
                //将秒杀的商品放入缓存
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(), seckillGoods);
                //保证商品不超卖 根据每一个商品的数量把商品按队列的形式放进redis中
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                }

                //发布一个消息 当所有商品的初始化状态为都是1
                //初始化所有商品都是可以秒杀的

                redisTemplate.convertAndSend("seckillpush", seckillGoods.getSkuId()+":1");
            }
            // 手动确认接收消息成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }

    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER, type = ExchangeTypes.DIRECT, durable = "true"),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckill(UserRecode userRecode, Message message, Channel channel) throws IOException {
        if (null != userRecode) {
            //Log.info("paySuccess:"+ JSONObject.toJSONString(userRecode));
            //预下单
            seckillGoodsService.seckillOrder(userRecode.getSkuId(), userRecode.getUserId());

            //确认收到消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
    /**
     * 秒杀结束清空缓存
     *
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK, type = ExchangeTypes.DIRECT, durable = "true"),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void clearRedis(Message message, Channel channel) throws IOException {

        //活动结束清空缓存
        QueryWrapper<SeckillGoods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.le("end_time", new Date());
        List<SeckillGoods> list = seckillGoodsMapper.selectList(queryWrapper);
        //清空缓存
        for (SeckillGoods seckillGoods : list) {
            redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId());
        }
        //清空秒杀的商品
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        //清空秒杀的订单
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        //删除用户秒杀的订单
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);
        //将状态更新为结束
        SeckillGoods seckillGoodsUp = new SeckillGoods();
        seckillGoodsUp.setStatus("2");
        seckillGoodsMapper.update(seckillGoodsUp, queryWrapper);
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
