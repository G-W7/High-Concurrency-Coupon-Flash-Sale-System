package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.LockImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author GW
 * @since 2026-9-29
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("秒杀券不存在！");
        }
        LocalDateTime now = LocalDateTime.now();
        //判断秒杀是否开始
        if (seckillVoucher.getBeginTime().isAfter(now)) {
            return Result.fail("秒杀尚未开始！");
        }
        //判断秒杀是否结束
        if (seckillVoucher.getEndTime().isBefore(now)) {
            return Result.fail("秒杀已经结束！");
        }
        //判断库存
        if (seckillVoucher.getStock() <= 0) {
            return Result.fail("库存不足！");
        }

        //上锁
        Long userId = UserHolder.getUser().getId();
        LockImpl lock = new LockImpl("order:" + userId, stringRedisTemplate);
        if (!lock.tryLock(1000)) {
            return Result.fail("请勿重复下单！");
        }

        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //判断用户是否已经购买过该优惠券
        Long userId = UserHolder.getUser().getId();

        int count = query()
                .eq("voucher_id", voucherId)
                .eq("user_id", userId)
                .count();
        if (count > 0) {
            return Result.fail("用户已经购买过该优惠券！");
        }
        //扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足！");
        }
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();

        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(orderId);

    }
}
