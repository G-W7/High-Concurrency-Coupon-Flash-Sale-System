package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * @author 虎哥
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher")
@Schema(description = "优惠券信息")
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "优惠券id", example = "10")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商铺id
     */
    @Schema(description = "关联商铺id", example = "1")
    private Long shopId;

    /**
     * 代金券标题
     */
    @Schema(description = "代金券标题", example = "100元代金券")
    private String title;

    /**
     * 副标题
     */
    @Schema(description = "副标题", example = "周一至周五均可使用")
    private String subTitle;

    /**
     * 使用规则
     */
    @Schema(description = "使用规则", example = "不可与其他优惠同享")
    private String rules;

    /**
     * 支付金额
     */
    @Schema(description = "支付金额（单位：分）", example = "8000")
    private Long payValue;

    /**
     * 抵扣金额
     */
    @Schema(description = "抵扣金额（单位：分）", example = "10000")
    private Long actualValue;

    /**
     * 优惠券类型
     */
    @Schema(description = "优惠券类型：1-代金券，2-秒杀券", example = "1")
    private Integer type;

    /**
     * 优惠券类型
     */
    @Schema(description = "状态：1-上架，2-下架，3-过期", example = "1")
    private Integer status;
    /**
     * 库存
     */
    @Schema(description = "库存（秒杀券，非数据库字段）", example = "100")
    @TableField(exist = false)
    private Integer stock;

    /**
     * 生效时间
     */
    @Schema(description = "生效时间（秒杀券，非数据库字段）", example = "2026-10-05 10:00:00")
    @TableField(exist = false)
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    @Schema(description = "失效时间（秒杀券，非数据库字段）", example = "2026-10-06 10:00:00")
    @TableField(exist = false)
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-10-05 10:00:00")
    private LocalDateTime createTime;


    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-10-05 12:00:00")
    private LocalDateTime updateTime;


}
