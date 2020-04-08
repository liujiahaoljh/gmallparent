package com.atguigu.gmall.mq.receiver;


import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author jiahao
 * @create 2020-03-30 20:42
 *
 * 获取消息 exchange.confirm routing.confirm 卢本伟牛逼
 */

@Component
@Configuration
public class ConfirmReceiver {
    @SneakyThrows //忽略异常
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(value = "queue.confirm",autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm",autoDelete = "true"),
            key = {"routing.confirm"}))
    public void process(Message message, Channel channel) {
        System.out.println("获取到的消息:" + new String(message.getBody()));
        //确认消息  第二个参数 false：每次值确认一条消息 true表示批量确认消息
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            //e.printStackTrace();
            //这有个方法 判断消息是否被消费过了
            if (message.getMessageProperties().getRedelivered()){
                System.out.println("消息已经处理过了，拒绝再次接收消费");
                //拒绝消息 不能重新进入队列
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);

            }else {
                System.out.println("消息再次进入队列");
                //第三个参数 表示是否回到队列  true回到队列
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            }

        }

    }
}
