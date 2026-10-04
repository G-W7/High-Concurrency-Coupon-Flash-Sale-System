package com.hmdp;

import cn.hutool.core.lang.UUID;
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
        String code = UUID.randomUUID().toString(true);
        System.out.println(code);
        }
    }