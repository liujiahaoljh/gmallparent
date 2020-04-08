package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import jodd.time.TimeUtil;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author jiahao
 * @create 2020-03-20 11:04
 * 测试：在缓存中存储一个num数据，模拟并发访问接口的方法
 */
@Service
public class TestServiceImpl implements TestService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    /*
        方法功能:数据累加
     */
    @Override
    public synchronized void testLock() {
        //自定义锁
        RLock lock = redissonClient.getLock("lock");
        //加锁
        //lock.lock(10,TimeUnit.SECONDS);
        try {
            boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
            if (res){
                //业务处理代码
                //查询缓存中num的key的数据
                String value = redisTemplate.opsForValue().get("num");
                //判断value是否有数据
                if (StringUtils.isBlank(value)){
                    return;
                }
                //将value 转化成Integer数据类型
                int num = Integer.parseInt(value);
                //将num放入缓存  并且自增
                redisTemplate.opsForValue().set("num", String.valueOf(++num));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }


        //释放锁
        //lock.unlock();


    }

    @Override
    public String readLock() {
        //声明锁对象
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("readwriteLock");
        //获取锁
        RLock rLock = rwlock.readLock();
        //加锁  10秒自动解锁
        rLock.lock(10,TimeUnit.SECONDS);
        String msg = redisTemplate.opsForValue().get("msg");
        return msg;
    }

    @Override
    public String writeLock() {
        //声明锁对象
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("readwriteLock");
        //获取锁
        RLock rLock = rwlock.writeLock();
        //加锁
        rLock.lock(10, TimeUnit.SECONDS);
        //写入数据
        redisTemplate.opsForValue().set("msg",UUID.randomUUID().toString());

        return "写入完成...";
    }

    private void  testRedis(){

        //声明一个uuid
        String uuid = UUID.randomUUID().toString();
        //加锁  setnx，del
        //在执行set的时候直接给出过期时间  保证命令的原子性
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "uuid",2, TimeUnit.SECONDS);
        //没有锁 加锁  处理数据
        if (lock){
            //查询缓存中num的key的数据
            String value = redisTemplate.opsForValue().get("num");
            //判断value是否有数据
            if (StringUtils.isBlank(value)){
                return;
            }
            //将value 转化成Integer数据类型
            int num = Integer.parseInt(value);
            //将num放入缓存  并且自增
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
            //声明script-lua脚本
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            //设置lua脚本返回的数据类型
            DefaultRedisScript redisScript = new DefaultRedisScript();
            //设置lua脚本返回的数据类型为Long
            redisScript.setResultType(Long.class);
            redisScript.setScriptText(script);
            redisTemplate.execute(redisScript, Arrays.asList("lock"),uuid);
            //打开锁
//            if (uuid.equals(redisTemplate.opsForValue().get("lock"))){
//                //是谁的锁  谁来删除  防止删除的时候误删
//                redisTemplate.delete("lock");
//            }

        }else {
            //其他线程需要等待
            try {
                Thread.sleep(1000);
                //睡醒了之后
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
