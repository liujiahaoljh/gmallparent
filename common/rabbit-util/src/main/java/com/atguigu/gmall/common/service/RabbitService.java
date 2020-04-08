package com.atguigu.gmall.common.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.entity.GmallCorrelationData;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.management.monitor.GaugeMonitor;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author jiahao
 * @create 2020-03-30 20:26
 *
 * 操作rabbitMq的一个工具类 封装了一个公共的方法
 */
@Service
public class RabbitService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;
    //过期时间：分钟
    public static final int OBJECT_TIMEOUT = 10;


    /**
     * //编写发送消息的方法
     * @param exchange 交换机
     * @param routingKey 路由
     * @param message 消息
     * @return
     */
    public boolean sendMessage(String exchange,String routingKey,Object message){
        //发送消息
        //rabbitTemplate.convertAndSend(exchange,routingKey,message);
        //封装好的数据对象
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        String correlationId = UUID.randomUUID().toString();
        gmallCorrelationData.setId(correlationId);
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        gmallCorrelationData.setMessage(message);

        //为了防止消息发送失败 在缓存中存储一份
        redisTemplate.opsForValue().set(correlationId, JSON.toJSONString(gmallCorrelationData),OBJECT_TIMEOUT, TimeUnit.MINUTES);



        //发送消息
        rabbitTemplate.convertAndSend(exchange,routingKey,message,gmallCorrelationData);

        //默认发送成功
        return true;

    }

    /**
     * 发送延迟消息
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param message 消息
     * @param delayTime 单位：秒
     */
    public boolean sendDelayMessage(String exchange, String routingKey, Object message, int delayTime) {
        GmallCorrelationData correlationData = new GmallCorrelationData();
        String correlationId = UUID.randomUUID().toString();
        correlationData.setId(correlationId);
        correlationData.setMessage(message);
        correlationData.setExchange(exchange);
        correlationData.setRoutingKey(routingKey);
        //确定开启延迟
        correlationData.setDelay(true);
        //设置延迟时间
        correlationData.setDelayTime(delayTime);

        redisTemplate.opsForValue().set(correlationId, JSON.toJSONString(correlationData), OBJECT_TIMEOUT, TimeUnit.MINUTES);
        //发送延迟消息内容
        this.rabbitTemplate.convertAndSend(exchange, routingKey, message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                //设置延迟时间
                message.getMessageProperties().setDelay(delayTime*1000);
                return message;
            }
        },correlationData);
        return true;
    }
}
