package com.hmdp.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author GW
 * @since 2026-10-9
 */
@Tag(name = "商铺管理", description = "商铺查询、新增、更新相关接口")
@RestController
@RequestMapping("/shop")
public class ShopController {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    public IShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @Operation(summary = "根据id查询商铺信息", description = "优先查询 Redis 缓存，未命中则查询数据库")
    @GetMapping("/{id}")
    public Result queryShopById(@Parameter(description = "商铺id", required = true, example = "1")
                                @PathVariable("id") Long id) {
        return shopService.queryById(id);
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @Operation(summary = "新增商铺信息", description = "写入数据库并返回商铺id")
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // 写入数据库
        shopService.save(shop);
        // 返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @Operation(summary = "更新商铺信息", description = "更新数据库并删除对应缓存")
    @PutMapping
    @Transactional
    public Result updateShop(@RequestBody Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 写入数据库
        shopService.updateById(shop);
        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @Operation(summary = "根据商铺类型分页查询商铺信息", description = "支持按地理坐标 x、y 排序")
    @GetMapping("/of/type")
    public Result queryShopByType(
            @Parameter(description = "商铺类型id", required = true, example = "1")
            @RequestParam("typeId") Integer typeId,
            @Parameter(description = "页码，默认1", example = "1")
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @Parameter(description = "经度", example = "120.15")
            @RequestParam(value = "x", required = false) Double x,
            @Parameter(description = "纬度", example = "30.28")
            @RequestParam(value = "y", required = false) Double y
    ) {

        return shopService.queryShopByType(typeId, current, x, y);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @Operation(summary = "根据商铺名称关键字分页查询商铺信息", description = "name 为空时查询全部")
    @GetMapping("/of/name")
    public Result queryShopByName(
            @Parameter(description = "商铺名称关键字", example = "美食")
            @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "页码，默认1", example = "1")
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
