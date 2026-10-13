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
 * @since 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_blog")
@Schema(description = "探店博客信息")
public class Blog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "博客主键id", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 商户id
     */
    @Schema(description = "关联商户id", example = "1")
    private Long shopId;
    /**
     * 用户id
     */
    @Schema(description = "发布用户id", example = "1")
    private Long userId;
    /**
     * 用户图标
     */
    @Schema(description = "用户头像（非数据库字段）", example = "https://example.com/icon.png")
    @TableField(exist = false)
    private String icon;
    /**
     * 用户姓名
     */
    @Schema(description = "用户昵称（非数据库字段）", example = "小鱼同学")
    @TableField(exist = false)
    private String name;
    /**
     * 是否点赞过了
     */
    @Schema(description = "当前用户是否已点赞（非数据库字段）", example = "false")
    @TableField(exist = false)
    private Boolean isLike;

    /**
     * 标题
     */
    @Schema(description = "博客标题", example = "探店打卡")
    private String title;

    /**
     * 探店的照片，最多9张，多张以","隔开
     */
    @Schema(description = "探店照片，最多9张，多张以','隔开", example = "1.png,2.png")
    private String images;

    /**
     * 探店的文字描述
     */
    @Schema(description = "探店文字描述", example = "这家店很好吃")
    private String content;

    /**
     * 点赞数量
     */
    @Schema(description = "点赞数量", example = "10")
    private Integer liked;

    /**
     * 评论数量
     */
    @Schema(description = "评论数量", example = "5")
    private Integer comments;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-10-06 10:00:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-10-06 12:00:00")
    private LocalDateTime updateTime;


}
