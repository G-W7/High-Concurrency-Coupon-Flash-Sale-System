package com.hcfs.service;

import com.hcfs.dto.Result;
import com.hcfs.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryTypeList();
}
