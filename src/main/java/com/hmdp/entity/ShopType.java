package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@TableName("tb_shop_type")
@Schema(description = "商铺类型信息")
public class ShopType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "商铺类型id", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类型名称
     */
    @Schema(description = "类型名称", example = "美食")
    private String name;

    /**
     * 图标
     */
    @Schema(description = "类型图标", example = "https://example.com/food.png")
    private String icon;

    /**
     * 顺序
     */
    @Schema(description = "排序顺序", example = "1")
    private Integer sort;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-10-09 10:00:00")
    @JsonIgnore
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-10-09 12:00:00")
    @JsonIgnore
    private LocalDateTime updateTime;


}
