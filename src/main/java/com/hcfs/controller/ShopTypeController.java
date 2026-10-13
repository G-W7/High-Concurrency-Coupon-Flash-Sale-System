package com.hcfs.controller;


import com.hcfs.dto.Result;
import com.hcfs.service.IShopTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author GW
 * @since 2026-10-13
 */
@Tag(name = "商铺类型管理", description = "商铺类型查询相关接口")
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @Operation(summary = "查询全部商铺类型", description = "优先从 Redis 缓存查询商铺类型列表")
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryTypeList();
    }
}
