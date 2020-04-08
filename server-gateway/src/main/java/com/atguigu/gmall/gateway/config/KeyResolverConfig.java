package com.atguigu.gmall.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * @author jiahao
 * @create 2020-04-07 20:25
 *
 * 限流原则上只采用一种
 */
@Configuration
public class KeyResolverConfig {

    //ip限流
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
    }

//    //userKey作为用户限流
//    @Bean
//    public KeyResolver userKeyResolver() {
//        return exchange -> Mono.just(exchange.getRequest().getQueryParams().getFirst("user"));
//    }
//
//    //api限流
//    @Bean
//    public KeyResolver apirKeyResolver() {
//        return exchange -> Mono.just(exchange.getRequest().getPath().value());
//    }


}