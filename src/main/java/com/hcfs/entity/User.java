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
@TableName("tb_user")
@Schema(description = "用户信息")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "用户id", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 手机号码
     */
    @Schema(description = "手机号码", example = "13800138000")
    private String phone;

    /**
     * 密码，加密存储
     */
    @Schema(description = "密码（加密存储）", example = "******")
    private String password;

    /**
     * 昵称，默认是随机字符
     */
    @Schema(description = "用户昵称，默认随机字符", example = "小鱼同学")
    private String nickName;

    /**
     * 用户头像
     */
    @Schema(description = "用户头像", example = "https://example.com/icon.png")
    private String icon = "";

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-10-08 10:00:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-10-08 12:00:00")
    private LocalDateTime updateTime;


}
