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
@TableName("tb_follow")
@Schema(description = "用户关注关系")
public class Follow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键id", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @Schema(description = "发起关注的用户id", example = "1")
    private Long userId;

    /**
     * 关联的用户id
     */
    @Schema(description = "被关注的用户id", example = "1010")
    private Long followUserId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-10-08 10:00:00")
    private LocalDateTime createTime;


}
