package com.hcfs.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hcfs.utils.RedisConstants.*;


/**
 * 缓存工具类
 * @author GW
 * @version 1.0
 * @since 2026-09-27
 */
@Component
@Slf4j
public class CacheClient {
    /**
     * Redis 字符串操作模板，用于缓存的读写与删除。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造函数注入 StringRedisTemplate。
     *
     * @param stringRedisTemplate Spring 提供的 Redis 操作模板
     */
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 缓存重建专用线程池，用于逻辑过期方案中异步重建缓存，避免阻塞请求线程。
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 设置缓存（普通缓存，带物理 TTL）。
     *
     * @param key   缓存的键
     * @param value 缓存的值，会被序列化为 JSON 字符串存储
     * @param time  过期时间数值
     * @param unit  过期时间单位
     */
    // 设置缓存
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 设置带逻辑过期时间的缓存。
     * <p>
     * 与 {@link #set} 不同，此方法不设置 Redis 的物理 TTL（即物理上永不过期），
     * 而是将过期时间封装在 {@link RedisData} 中，由业务逻辑判断是否过期，
     * 主要用于解决缓存击穿问题。
     *
     * @param key   缓存的键
     * @param value 缓存的业务数据
     * @param time  逻辑过期时间数值（相对于当前时间的时长）
     * @param unit  逻辑过期时间单位
     */
    // 设置带逻辑过期时间的缓存
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis，不设置TTL（物理永不过期）
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存查询——缓存穿透解决方案（空值缓存）。
     * <p>
     * 处理流程：
     * <ol>
     *     <li>查询 Redis，命中有效数据则直接返回；</li>
     *     <li>命中空字符串（此前缓存的空值），说明数据库中不存在该数据，返回 {@code null}；</li>
     *     <li>缓存未命中则查询数据库，数据库也不存在时缓存空值（短 TTL）以防穿透；</li>
     *     <li>数据库存在则写入缓存并返回。</li>
     * </ol>
     *
     * @param keyPrefix  缓存键前缀
     * @param id         业务主键 ID
     * @param type       返回值类型，用于反序列化
     * @param dbFallback 缓存未命中时的数据库查询逻辑
     * @param time       缓存过期时间数值
     * @param unit       缓存过期时间单位
     * @param <R>        返回数据类型
     * @param <ID>       主键类型
     * @return 查询到的对象，不存在时返回 {@code null}
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 从Redis中查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 判断缓存是否存在
        if (StrUtil.isNotBlank(json)) {
            // 返回缓存数据
            return JSONUtil.toBean(json, type);
        }
        if ("".equals(json)) {
            // 命中空值缓存，说明数据库中不存在，直接返回 null，防止缓存穿透
            return null;
        }
        R r = dbFallback.apply(id);
        if (r == null) {
            // 数据库也不存在，缓存空值（短 TTL）以防穿透
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        set(key, r, time, unit);
        return r;
    }

    /**
     * 缓存查询——缓存击穿解决方案（逻辑过期）。
     * <p>
     * 缓存物理上永不过期，通过 {@link RedisData} 中记录的逻辑过期时间判断是否需要重建：
     * <ol>
     *     <li>Redis 未命中直接返回 {@code null}（需提前预热缓存）；</li>
     *     <li>命中且逻辑未过期，直接返回；</li>
     *     <li>命中但已过期，尝试获取互斥锁，成功则开启独立线程异步重建缓存；</li>
     *     <li>无论是否重建，均返回当前（可能已过期的）数据，保证请求不被阻塞。</li>
     * </ol>
     *
     * @param keyPrefix  缓存键前缀
     * @param id         业务主键 ID
     * @param type       返回值类型，用于反序列化
     * @param dbFallback 缓存重建时的数据库查询逻辑
     * @param time       逻辑过期时间数值
     * @param unit       逻辑过期时间单位
     * @param <R>        返回数据类型
     * @param <ID>       主键类型
     * @return 查询到的对象（可能是已过期的旧数据），缓存不存在时返回 {@code null}
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isBlank(json)) {
            // 3.不存在，直接返回（逻辑过期方案需提前预热缓存）
            return null;
        }
        // 4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            // 5.1.未过期，直接返回店铺信息
            return r;
        }
        // 5.2.已过期，需要缓存重建
        // 6.缓存重建
        // 6.1.获取互斥锁
        String lockKey = LOCK_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2.判断是否获取锁成功
        if (isLock){
            // 6.3.成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R newR = dbFallback.apply(id);
                    // 重建缓存
                    this.setWithLogicalExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        // 6.4.返回过期的商铺信息
        return r;
    }

    /**
     * 缓存查询——缓存击穿解决方案（互斥锁）。
     * <p>
     * 缓存未命中时，通过互斥锁保证同一时刻只有一个线程重建缓存，其余线程休眠后重试，
     * 从而避免大量请求同时打到数据库（缓存击穿）。同时结合空值缓存防止缓存穿透。
     *
     * @param keyPrefix  缓存键前缀
     * @param id         业务主键 ID
     * @param type       返回值类型，用于反序列化
     * @param dbFallback 缓存未命中时的数据库查询逻辑
     * @param time       缓存过期时间数值
     * @param unit       缓存过期时间单位
     * @param <R>        返回数据类型
     * @param <ID>       主键类型
     * @return 查询到的对象，不存在时返回 {@code null}
     */
    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            // 命中空值缓存，说明数据库中不存在，返回 null 防止缓存穿透
            return null;
        }

        // 4.实现缓存重建
        // 4.1.获取互斥锁
        String lockKey = LOCK_KEY + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2.判断是否获取成功
            if (!isLock) {
                // 4.3.获取锁失败，休眠并重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            // 4.4.获取锁成功，根据id查询数据库
            r = dbFallback.apply(id);
            // 5.不存在，返回错误
            if (r == null) {
                // 将空值写入redis，防止缓存穿透
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            // 6.存在，写入redis
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 7.释放锁
            unlock(lockKey);
        }
        // 8.返回
        return r;
    }

    /**
     * 尝试获取分布式互斥锁。
     * <p>
     * 利用 Redis 的 {@code setIfAbsent}（即 SETNX）保证原子性，成功设置即代表获取锁成功。
     *
     * @param key 锁的键
     * @return 获取锁成功返回 {@code true}，否则返回 {@code false}
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放分布式互斥锁。
     *
     * @param key 锁的键
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}

