package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jiahao
 * @create 2020-03-31 19:07
 */
@Configuration
public class DeadLetterMqConfig {
    //生命一个交换机，路由键，队列

    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    /**
     * 其他队列可以在RabbitListener上面做绑定
     * @return
     */

    //声明一个交换机并且注入到spring容器中
    @Bean
    public DirectExchange exchange(){

        //第二个参数：是否需要持久化 第三个：是否自动删除
        return new DirectExchange(exchange_dead, true, false, null);
    }

    //定义一个队列
    @Bean
    public Queue queue1(){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", exchange_dead);
        arguments.put("x-dead-letter-routing-key", routing_dead_2);
        //针对全局的过期时间
        arguments.put("x-message-ttl", 10 * 1000);
        //第三个参数：是不是唯一所属
        return new Queue(queue_dead_1, true, false, false, arguments);
    }

    //定义绑定规则
    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }

    //定义第二个队列
    @Bean
    public Queue queue2(){
        return new Queue(queue_dead_2, true, false, false, null);
    }

    @Bean
    public Binding deadBinding(){
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }
}