package com.hcfs.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    private static final long START_STAMP = 1640995200L;
    private static final long SEQUENCE_BIT = 32;
    private final StringRedisTemplate stringRedisTemplate;
    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public long nextId(String keyPrefix) {
        //1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = System.currentTimeMillis() / 1000L;
        long timeStamp = nowSecond - START_STAMP;
        //2. 获取序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long sequence = stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix +":"+ date);
        //3. 组合
        return (timeStamp << SEQUENCE_BIT) | sequence;
    }
}
