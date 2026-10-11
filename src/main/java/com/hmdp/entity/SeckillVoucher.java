package com.hmdp.entity;

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
 * 秒杀优惠券表，与优惠券是一对一关系
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_seckill_voucher")
@Schema(description = "秒杀优惠券信息")
public class SeckillVoucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联的优惠券的id
     */
    @Schema(description = "关联的优惠券id", example = "10")
    @TableId(value = "voucher_id", type = IdType.INPUT)
    private Long voucherId;

    /**
     * 库存
     */
    @Schema(description = "秒杀库存", example = "100")
    private Integer stock;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-10-05 10:00:00")
    private LocalDateTime createTime;

    /**
     * 生效时间
     */
    @Schema(description = "秒杀开始时间", example = "2026-10-05 10:00:00")
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    @Schema(description = "秒杀结束时间", example = "2026-10-06 10:00:00")
    private LocalDateTime endTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-10-05 12:00:00")
    private LocalDateTime updateTime;


}
