package com.hcfs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher_order")
@Schema(description = "秒杀订单信息")
public class VoucherOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "订单id（全局唯一）", example = "1234567890123456789")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 下单的用户id
     */
    @Schema(description = "下单用户id", example = "1")
    private Long userId;

    /**
     * 购买的代金券id
     */
    @Schema(description = "购买的代金券id", example = "10")
    private Long voucherId;

    /**
     * 支付方式 1：余额支付；2：支付宝；3：微信
     */
    @Schema(description = "支付方式：1-余额，2-支付宝，3-微信", example = "1")
    private Integer payType;

    /**
     * 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
     */
    @Schema(description = "订单状态：1-未支付，2-已支付，3-已核销，4-已取消，5-退款中，6-已退款", example = "1")
    private Integer status;

    /**
     * 下单时间
     */
    @Schema(description = "下单时间", example = "2026-10-05 10:00:00")
    private LocalDateTime createTime;

    /**
     * 支付时间
     */
    @Schema(description = "支付时间", example = "2026-10-05 10:05:00")
    private LocalDateTime payTime;

    /**
     * 核销时间
     */
    @Schema(description = "核销时间", example = "2026-10-05 12:00:00")
    private LocalDateTime useTime;

    /**
     * 退款时间
     */
    @Schema(description = "退款时间", example = "2026-10-05 13:00:00")
    private LocalDateTime refundTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-10-05 12:00:00")
    private LocalDateTime updateTime;


}
