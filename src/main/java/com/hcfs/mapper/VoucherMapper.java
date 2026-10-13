package com.hcfs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hcfs.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
