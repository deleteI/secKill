package com.nnngu.service.redis;

import com.nnngu.entity.Seckill;

public interface RedisService {

    public void put (long key,Seckill value,long seconds);

    public Seckill get(long key);

}
