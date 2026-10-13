package com.hcfs;

import com.hcfs.entity.Shop;
import com.hcfs.service.IShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hcfs.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class HCFSlicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IShopService shopService;

    @Test
    void loadShopData() {
        // 把店铺的经纬坐标以及 id 按 typeId 分组，批量加载到 Redis 的 GEO 结构中
        List<Shop> shops = shopService.list();
        // 过滤掉坐标或类型缺失的脏数据，并按 typeId 分组
        Map<Long, List<Shop>> shopMap = shops.stream()
                .filter(shop -> shop.getTypeId() != null
                        && shop.getX() != null
                        && shop.getY() != null)
                .collect(Collectors.groupingBy(Shop::getTypeId));

        // 每种类型对应一个独立的 GEO key：shop:geo:{typeId}
        shopMap.forEach((typeId, shopList) -> {
            String key = SHOP_GEO_KEY + typeId;
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(shopList.size());
            for (Shop shop : shopList) {
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())));
            }
            // 同一类型的店铺一次性批量写入，减少网络往返
            stringRedisTemplate.opsForGeo().add(key, locations);
        });
    }
}