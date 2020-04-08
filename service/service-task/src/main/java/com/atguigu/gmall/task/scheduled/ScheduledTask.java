package com.atguigu.gmall.task.scheduled;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.entity.GmallCorrelationData;
import com.atguigu.gmall.common.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author jiahao
 * @create 2020-03-31 15:00
 */
@Component
@EnableScheduling  //开启定时任务
@Slf4j
public class ScheduledTask {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitService rabbitService;

    @Scheduled(cron = "0/20 * *  * * ?") //定义规则 分 时 日 周 月 年
    public void task() {
        log.info("每30秒执行一次");
        //需要重新发送的消息
        String msg = (String) redisTemplate.opsForList().rightPop(MqConst.MQ_KEY_PREFIX);
        //如果缓存中数据为空 则返回
        if (StringUtils.isEmpty(msg)) return;
        //不为空 将msg消息转化成为对象
        GmallCorrelationData gmallCorrelationData = JSON.parseObject(msg, GmallCorrelationData.class);

        if (gmallCorrelationData.isDelay()) {
            //处理延迟消息
            rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(), gmallCorrelationData.getRoutingKey(), gmallCorrelationData.getMessage(), message -> {
                message.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime() * 1000);
                return message;
            }, gmallCorrelationData);

        } else {
            // 处理非延迟消息
            // 再次发送消息
            rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(), gmallCorrelationData.getRoutingKey(), gmallCorrelationData.getMessage(), gmallCorrelationData);
        }
    }

    /**
     * 每天凌晨1点执行
     */
    //@Scheduled(cron = "0/30 * * * * ?")
    @Scheduled(cron = "0 0 1 * * ?")
    public void task1() {
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_1, "");
    }

    @Scheduled(cron = "0 0 18 * * ?")
    public void task18() {
        log.info("task18");
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_18, "");
    }
}