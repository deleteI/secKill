package com.nnngu.service;

import com.nnngu.entity.Seckill;
import com.nnngu.service.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {

    @Autowired
    private  RedisTemplate redisTemplate;

    /**
     *
     * @param key 键
     * @param value 值
     * @param seconds 缓冲时间
     */
    @Override
    public void put(long key, Seckill value, long seconds) {
        redisTemplate.opsForValue().set(key,value,seconds, TimeUnit.SECONDS);
    }

    @Override
    public Seckill get(long key) {
        return (Seckill) redisTemplate.opsForValue().get(key);
    }
}
