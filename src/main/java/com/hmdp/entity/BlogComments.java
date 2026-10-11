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
 * 
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_blog_comments")
@Schema(description = "博客评论信息")
public class BlogComments implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "评论主键id", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @Schema(description = "评论用户id", example = "1")
    private Long userId;

    /**
     * 探店id
     */
    @Schema(description = "关联博客id", example = "1")
    private Long blogId;

    /**
     * 关联的1级评论id，如果是一级评论，则值为0
     */
    @Schema(description = "关联的一级评论id，一级评论则为0", example = "0")
    private Long parentId;

    /**
     * 回复的评论id
     */
    @Schema(description = "回复的评论id", example = "0")
    private Long answerId;

    /**
     * 回复的内容
     */
    @Schema(description = "评论内容", example = "看起来很好吃")
    private String content;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数", example = "0")
    private Integer liked;

    /**
     * 状态，0：正常，1：被举报，2：禁止查看
     */
    @Schema(description = "状态：0-正常，1-被举报，2-禁止查看", example = "0")
    private Boolean status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-10-11 10:00:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-10-11 12:00:00")
    private LocalDateTime updateTime;


}
