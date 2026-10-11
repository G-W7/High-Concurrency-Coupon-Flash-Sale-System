package com.hmdp.controller;


import com.hmdp.service.IVoucherOrderService;
import com.hmdp.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author GW
 * @since 2026-10-5
 */
@Tag(name = "秒杀订单管理", description = "优惠券秒杀下单相关接口")
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Autowired
    private IVoucherOrderService voucherOrderService;

    @Operation(summary = "秒杀下单", description = "根据优惠券id执行秒杀下单，一人一单")
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@Parameter(description = "秒杀优惠券id", required = true, example = "10")
                                 @PathVariable("id") Long voucherId) {
        Result result = voucherOrderService.seckillVoucher(voucherId);
        return result;
    }
}
