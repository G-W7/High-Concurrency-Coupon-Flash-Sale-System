package com.hmdp;

import cn.hutool.core.util.RandomUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HmDianPingApplicationTests {

    @Test
    void testRandomCode() {
        String code = RandomUtil.randomNumbers(6);
        System.out.println(code);
    }

}
