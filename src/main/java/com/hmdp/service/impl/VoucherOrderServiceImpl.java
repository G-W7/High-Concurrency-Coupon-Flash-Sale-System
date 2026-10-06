package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 秒杀优惠券订单服务实现类
 * <p>
 * 核心架构：基于 Redis Stream 消息队列的异步秒杀下单方案
 * 1. 用户请求到达时，通过 Lua 脚本在 Redis 中原子性地完成：库存预扣、一人一单校验、消息投递
 * 2. 异步消费线程从 Redis Stream 中拉取订单消息，通过分布式锁 + 数据库事务完成真正的下单操作
 * </p>
 *
 * @author GW
 * @since 2026-9-29
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    /** 全局唯一 ID 生成器，基于 Redis 自增实现 */
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /** Redisson 客户端，用于获取分布式锁 */
    @Autowired
    private RedissonClient redisson;

    /**
     * Spring AOP 代理对象，用于在异步线程中调用带 @Transactional 的方法。
     * 通过 @Lazy 自注入获取被 AOP 包装后的代理对象，打破自引用循环依赖。
     * 如果直接通过 this 调用，会绕过代理导致事务注解失效。
     */
    @Autowired
    @Lazy
    private IVoucherOrderService proxy;

    /**
     * 秒杀 Lua 脚本（静态加载，全局复用）
     * 脚本逻辑：
     *   返回 0 → 抢购成功（已扣库存、已记录用户、已投递消息到 Stream）
     *   返回 1 → 库存不足
     *   返回 2 → 用户重复下单
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /** 单线程池，用于运行异步订单消费任务（保证同一时刻只有一个线程在处理订单） */
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * Bean 初始化完成后执行：
     * 提交异步消费任务到线程池。
     * proxy 已由 @Lazy 自注入完成，无需再调用 AopContext.currentProxy()
     * （该方法在 @PostConstruct 阶段无 AOP 调用上下文，会抛 IllegalStateException）。
     */
    @PostConstruct
    public void init() {
        // 确保 Stream key 存在：写入一条初始消息后立即删除，以此触发 Stream 创建
        String streamKey = "stream.orders";
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(streamKey))) {
            RecordId recordId = stringRedisTemplate.opsForStream().add(
                    StreamRecords.newRecord().in(streamKey).ofMap(Collections.singletonMap("init", "1"))
            );
            stringRedisTemplate.opsForStream().delete(streamKey, recordId);
        }
        // 确保消费者组存在（已存在时抛 BUSYGROUP 异常，忽略即可）
        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, "g1");
        } catch (Exception ignored) {
            log.debug("消费者组 g1 已存在，跳过创建");
        }
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderTask());
    }

    /**
     * 异步订单消费任务（内部类）
     * 持续从 Redis Stream "stream.orders" 中拉取消息并处理，实现异步下单。
     * 使用消费者组 "g1"，消费者 "c1"。
     */
    private class VoucherOrderTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // 从 Stream 中阻塞读取一条消息（最多等待 2 秒）
                    // ReadOffset.lastConsumed() 表示从消费者组上次消费的位置之后开始读取（即 ">"）
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
                    );
                    // 如果没有读取到消息，继续下一轮循环
                    if (list == null || list.isEmpty()) {
                        continue;
                    }
                    // 取出消息体，转换为 VoucherOrder 实体对象
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 处理订单（加分布式锁 + 调用事务方法写入数据库）
                    handleVoucherOrder(voucherOrder);
                    // 处理成功后手动确认消息（ACK），从 Pending List 中移除
                    stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    // 出现异常时，尝试重新处理 Pending List 中未确认的消息
                    handlePendingList();
                }
            }
        }
    }

    /**
     * 处理 Pending List（未确认消息列表）
     * 当正常消费出现异常时调用，从 Stream 的头部（offset "0"）重新读取未被 ACK 的消息进行重试。
     * 实现"至少一次"消费语义，确保消息不会丢失。
     */
    public void handlePendingList() {
        while (true) {
            try {
                // 从 "0" 开始读取，获取所有已投递但未确认的消息
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create("stream.orders", ReadOffset.from("0"))
                );
                // 如果没有待处理消息，说明 Pending List 已清空，退出循环
                if (list == null || list.isEmpty()) {
                    break;
                }
                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> value = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                handleVoucherOrder(voucherOrder);
                // 重试成功后确认消息
                stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", record.getId());
            } catch (Exception e) {
                log.error("处理pending订单异常", e);
                // 短暂休眠避免无限循环时 CPU 空转
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * 处理单个优惠券订单
     * 使用 Redisson 分布式锁保证同一用户的订单串行处理（防止并发导致一人多单）
     * 通过 proxy（AOP 代理对象）调用 createVoucherOrder，确保 @Transactional 事务生效
     */
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 以用户 ID 为粒度加分布式锁
        RLock lock = redisson.getLock("lock:order:" + userId);
        boolean isLocked = false;
        try {
            // tryLock(等待时间1秒, 自动释放时间10秒)
            isLocked = lock.tryLock(1, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!isLocked) {
            log.error("用户 {} 获取锁失败", userId);
            return;
        }
        try {
            // 必须通过代理对象调用，否则 @Transactional 不生效
            proxy.createVoucherOrder(voucherOrder);
        } catch (Exception e) {
            log.error("创建订单异常", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 秒杀优惠券入口方法（同步部分）
     * <p>
     * 执行流程：
     * 1. 获取当前登录用户 ID
     * 2. 生成全局唯一订单 ID
     * 3. 执行 Lua 脚本，在 Redis 中原子性完成：
     *    - 校验库存是否充足
     *    - 校验用户是否重复下单（一人一单）
     *    - 预扣库存
     *    - 将用户 ID 加入已购集合
     *    - 将订单信息投递到 Redis Stream 消息队列
     * 4. 根据 Lua 脚本返回值判断结果
     * </p>
     * 注意：此方法不涉及任何数据库操作，所有判断都在 Redis 中完成，保证高性能
     */
    @Override
    public Result seckillVoucher(@NonNull Long voucherId) {
        // 从 ThreadLocal 中获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 生成全局唯一订单 ID（基于 Redis 自增 + 时间戳）
        long orderId = redisIdWorker.nextId("order");
        // 执行 Lua 脚本，参数通过 ARGV 传递（无需 KEYS，脚本内部自行拼接 key）
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(),
                String.valueOf(orderId)
        );
        // 防御性编程：Lua 脚本执行失败时 result 可能为 null
        int r = result == null ? 1 : result.intValue();
        if (r != 0) {
            // 返回错误信息：r==1 库存不足，r==2 重复下单
            return Result.fail(r == 1 ? "库存不足！" : "请勿重复下单!!");
        }
        // 抢购成功，返回预生成的订单 ID（此时订单尚未写入数据库，由异步线程完成）
        return Result.ok(orderId);
    }

    /**
     * 创建秒杀订单（异步部分，由消费线程通过代理调用）
     * <p>
     * 执行流程：
     * 1. 二次校验一人一单（数据库层面，防止 Redis 与 DB 状态不一致）
     * 2. 使用乐观锁扣减数据库库存（WHERE stock > 0）
     * 3. 如果数据库扣库存失败，回滚 Redis 中的预扣状态（保证一致性）
     * 4. 保存订单到数据库
     * </p>
     * 注意：@Transactional 必须通过 AOP 代理调用才能生效
     */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 数据库层面二次校验：查询该用户是否已购买过该优惠券
        long count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0) {
            log.error("用户 {} 已经购买过该优惠券！", userId);
            return;
        }
        // 使用乐观锁（CAS）扣减数据库库存：stock = stock - 1 WHERE stock > 0
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            // 数据库库存扣减失败（可能是 Redis 与 DB 库存不同步）
            // 回滚 Redis 状态：恢复预扣的库存，移除用户的购买标记
            log.error("库存不足！");
            Long voucherId = voucherOrder.getVoucherId();
            stringRedisTemplate.opsForValue().increment("seckill:stock:" + voucherId);
            stringRedisTemplate.opsForSet().remove("seckill:order:" + voucherId, userId.toString());
            return;
        }
        // 扣库存成功，保存订单到数据库
        save(voucherOrder);
    }


}

