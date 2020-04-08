package com.atguigu.gmall.common.cache;


import java.lang.annotation.*;

/**
 * 说明该注解在什么位置上使用  在方法上面使用
 * 说明该注解的声明周期是多长  在java 源文件，class文件，jvm 都存在！
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {
    // 自定义属性 set(key,value) 给一个前缀 默认值
    String prefix() default "cache";
}
