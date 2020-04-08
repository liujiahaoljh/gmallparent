package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author jiahao
 * @create 2020-03-31 19:23
 */
@Component
@Configuration
public class DeadLetterReceiver {

    //监听消息
    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void get(String msg) {
        System.out.println("Receive:" + msg);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Receive queue_dead_2: " + sdf.format(new Date()) + " Delay rece." + msg);
    }

}