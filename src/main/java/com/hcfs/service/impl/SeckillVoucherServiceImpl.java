package com.hcfs.service.impl;

import com.hcfs.entity.SeckillVoucher;
import com.hcfs.mapper.SeckillVoucherMapper;
import com.hcfs.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 秒杀优惠券服务实现类
 * <br>
 * 对应秒杀优惠券表（与优惠券一对一关系），记录库存、秒杀起止时间等信息。
 * 继承 MyBatis-Plus 的 {@link ServiceImpl} 复用通用 CRUD 能力，
 * 秒杀下单时的库存扣减由 {@code VoucherOrderServiceImpl} 直接调用其链式方法完成。
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

}
