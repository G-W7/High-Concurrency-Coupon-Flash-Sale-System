package com.hcfs.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcfs.dto.Result;
import com.hcfs.entity.Shop;
import com.hcfs.mapper.ShopMapper;
import com.hcfs.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hcfs.utils.CacheClient;
import com.hcfs.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hcfs.utils.RedisConstants.*;

/**
 * <p>
 * 商铺服务实现类
 * <br>
 * 负责商铺的缓存查询（集成缓存穿透 / 击穿解决方案）以及基于 Redis GEO 的
 * 附近商铺按距离排序分页查询。缓存逻辑委托给通用工具类 {@link CacheClient} 实现。
 * </p>
 *
 * @author GW
 * @since 2026-10-10
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    /** 通用缓存客户端，封装了缓存穿透、击穿（互斥锁 / 逻辑过期）等解决方案 */
    @Autowired
    private CacheClient cacheClient;

    /** Redis 操作模板，用于 GEO 地理位置查询附近的商铺 */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据 id 查询商铺信息（带缓存）
     * <br>
     * 默认采用缓存空值方案预防缓存穿透；如需应对缓存击穿，
     * 可切换为互斥锁（queryWithMutex）或逻辑过期（queryWithLogicalExpire）方案。
     *
     * @param id 商铺 id
     * @return 封装了商铺信息的统一返回结果；商铺不存在时返回失败
     */
    @Override
    public Result queryById(Long id) {
        //预防缓存穿透
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //预防缓存击穿
        //Shop shop = cacheClient.queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //预防缓存击穿逻辑过期时间，设置逻辑过期时间
        //Shop shop=cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    /**
     * 根据商铺类型分页查询商铺，支持按地理坐标排序（附近商铺）
     * <br>
     * 当传入坐标 x、y 时，使用 Redis GEO 查询并按距离升序排序、分页，
     * 通过 ORDER BY FIELD 保证数据库查询结果与 GEO 的距离排序一致；
     * 未传坐标时则退化为普通数据库分页查询。
     *
     * @param typeId  商铺类型 id
     * @param current 当前页码
     * @param x       经度（可选，为空时不按坐标查询）
     * @param y       纬度（可选，为空时不按坐标查询）
     * @return 封装了商铺列表的统一返回结果；带坐标查询时结果含 distance 字段
     */
    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标，直接按数据库分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        // 2.计算分页参数（GEO 只能 limit，需要自己算 from ~ end）
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3.查询 Redis GEO，按距离排序并分页，结果包含 shopId 和 distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000, Metrics.KILOMETERS),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                                .includeDistance()
                                .sortAscending()
                                .limit(end));

        // 4.解析出 id 与距离
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            // 没有下一页了，直接返回空集合
            return Result.ok(Collections.emptyList());
        }
        // 4.1.截取 from ~ end 部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            // 4.2.获取店铺 id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });

        // 5.根据 id 批量查询 Shop，并用 ORDER BY FIELD 保持 GEO 的距离排序
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids)
                .last("ORDER BY FIELD(id," + idStr + ")")
                .list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }

        // 6.返回结果
        return Result.ok(shops);
    }
}

