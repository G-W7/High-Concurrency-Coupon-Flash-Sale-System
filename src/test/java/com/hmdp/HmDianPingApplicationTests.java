package com.hmdp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Test
    void testRandomCode() {
        long START_STAMP = 1640995200L;
        long SEQUENCE_BIT = 32;



            String keyPrefix = "shop";
            //1. 生成时间戳
            LocalDateTime now = LocalDateTime.now();
            long nowSecond = System.currentTimeMillis() / 1000L;
            long timeStamp = nowSecond - START_STAMP;
            //2. 获取序列号
            String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
            long sequence = stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix +":"+ date);
            //3. 组合
            System.out.println(sequence);
            System.out.println((timeStamp << SEQUENCE_BIT) | sequence);
        }
    }