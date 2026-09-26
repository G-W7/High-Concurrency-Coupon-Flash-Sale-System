package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author GW
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
