package com.hcfs.entity;

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
 * @author GW
 * @since 2026-10-9
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_shop")
@Schema(description = "商铺信息")
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "商铺主键id", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商铺名称
     */
    @Schema(description = "商铺名称", example = "美食小当家")
    private String name;

    /**
     * 商铺类型的id
     */
    @Schema(description = "商铺类型id", example = "1")
    private Long typeId;

    /**
     * 商铺图片，多个图片以','隔开
     */
    @Schema(description = "商铺图片，多个图片以','隔开", example = "https://example.com/1.png")
    private String images;

    /**
     * 商圈，例如陆家嘴
     */
    @Schema(description = "商圈，例如陆家嘴", example = "陆家嘴")
    private String area;

    /**
     * 地址
     */
    @Schema(description = "商铺地址", example = "上海市浦东新区世纪大道100号")
    private String address;

    /**
     * 经度
     */
    @Schema(description = "经度", example = "121.50")
    private Double x;

    /**
     * 维度
     */
    @Schema(description = "纬度", example = "31.24")
    private Double y;

    /**
     * 均价，取整数
     */
    @Schema(description = "人均价格，取整数", example = "80")
    private Long avgPrice;

    /**
     * 销量
     */
    @Schema(description = "销量", example = "1000")
    private Integer sold;

    /**
     * 评论数量
     */
    @Schema(description = "评论数量", example = "200")
    private Integer comments;

    /**
     * 评分，1~5分，乘10保存，避免小数
     */
    @Schema(description = "评分，1~5分乘10保存，避免小数", example = "45")
    private Integer score;

    /**
     * 营业时间，例如 10:00-22:00
     */
    @Schema(description = "营业时间，例如 10:00-22:00", example = "10:00-22:00")
    private String openHours;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-10-09 10:00:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-10-09 12:00:00")
    private LocalDateTime updateTime;


    @Schema(description = "距离（非数据库字段，按坐标计算）", example = "1.2")
    @TableField(exist = false)
    private Double distance;
}
