package com.hcfs.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.hcfs.dto.Result;
import com.hcfs.entity.ShopType;
import com.hcfs.mapper.ShopTypeMapper;
import com.hcfs.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hcfs.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.hcfs.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 * 商铺类型服务实现类
 * <br>
 * 负责商铺类型列表的缓存查询。采用 Redis Hash 结构缓存整个类型列表
 * （field 为 sort 排序值，value 为 JSON 字符串），并在 TTL 上叠加随机值以防缓存雪崩。
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    /** Redis 操作模板，用于以 Hash 结构缓存商铺类型列表 */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询商铺类型列表（带 Redis Hash 缓存）
     * <br>
     * 流程：先查 Redis Hash 缓存，命中则反序列化并按 sort 排序返回；
     * 未命中则查询数据库，再将结果以 Hash 结构写入 Redis 并设置随机 TTL（防雪崩）。
     *
     * @return 封装了按 sort 升序排列的商铺类型列表的统一返回结果
     */
    @Override
    public Result queryTypeList() {
        // 1.查询 Redis Hash 缓存
        Map<String, String> typeMap = stringRedisTemplate.<String, String>opsForHash().entries(CACHE_SHOP_TYPE_KEY);
        // 2.缓存命中，反序列化并按 sort 排序后返回
        if (!typeMap.isEmpty()) {
            List<ShopType> typeList = typeMap.values().stream()
                    .map(json -> JSONUtil.toBean(json, ShopType.class))
                    .sorted(Comparator.comparing(ShopType::getSort))
                    .collect(Collectors.toList());
            return Result.ok(typeList);
        }
        // 3.缓存未命中，查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        // 4.数据库也无数据，直接返回空列表
        if (typeList == null || typeList.isEmpty()) {
            return Result.ok(typeList);
        }
        // 5.将数据库数据以 Hash 结构写入 Redis（field 为 sort，value 为 JSON）
        Map<String, String> cacheMap = typeList.stream()
                .collect(Collectors.toMap(
                        type -> type.getSort().toString(),
                        JSONUtil::toJsonStr));
        stringRedisTemplate.<String, String>opsForHash().putAll(CACHE_SHOP_TYPE_KEY, cacheMap);
        long ttl = CACHE_SHOP_TTL + RandomUtil.randomInt(5);
        stringRedisTemplate.expire(CACHE_SHOP_TYPE_KEY, ttl, TimeUnit.MINUTES);
        // 6.返回结果
        return Result.ok(typeList);
    }
}
