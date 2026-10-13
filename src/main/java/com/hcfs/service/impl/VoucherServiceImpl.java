package com.hcfs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hcfs.dto.Result;
import com.hcfs.entity.Voucher;
import com.hcfs.mapper.VoucherMapper;
import com.hcfs.entity.SeckillVoucher;
import com.hcfs.service.ISeckillVoucherService;
import com.hcfs.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.hcfs.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 * 优惠券服务实现类
 * <br>
 * 负责查询商铺关联的优惠券列表，以及新增秒杀优惠券。
 * 新增秒杀券时会同时写入优惠券表、秒杀券表，并将库存同步到 Redis，
 * 供秒杀下单时进行高性能的库存预扣。
 * </p>
 *
 * @author GW
 * @since 2026-10-4
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    /** Redis 操作模板，用于缓存秒杀券库存 */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 秒杀优惠券服务，用于保存秒杀券的库存与起止时间等信息 */
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    /**
     * 查询指定商铺关联的优惠券列表
     *
     * @param shopId 商铺 id
     * @return 封装了优惠券列表的统一返回结果
     */
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    /**
     * 新增秒杀优惠券
     * <br>
     * 在事务中依次保存优惠券、秒杀券信息，并将库存写入 Redis（key 为 SECKILL_STOCK_KEY + voucherId），
     * 为后续秒杀下单的库存预扣做准备。
     *
     * @param voucher 优惠券对象（含库存、秒杀起止时间等秒杀相关信息）
     */
    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        //保存秒杀库存到Redis
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + seckillVoucher.getVoucherId(), seckillVoucher.getStock().toString());
    }
}
