package com.hcfs.service;

import com.hcfs.dto.Result;
import com.hcfs.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
