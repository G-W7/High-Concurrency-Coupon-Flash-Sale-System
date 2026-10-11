package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user_info")
@Schema(description = "用户详情信息")
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，用户id
     */
    @Schema(description = "用户id", example = "1")
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 城市名称
     */
    @Schema(description = "城市名称", example = "上海")
    private String city;

    /**
     * 个人介绍，不要超过128个字符
     */
    @Schema(description = "个人介绍，不超过128字符", example = "爱美食爱生活")
    private String introduce;

    /**
     * 粉丝数量
     */
    @Schema(description = "粉丝数量", example = "100")
    private Integer fans;

    /**
     * 关注的人的数量
     */
    @Schema(description = "关注人数", example = "50")
    private Integer followee;

    /**
     * 性别，0：男，1：女
     */
    @Schema(description = "性别：0-男，1-女", example = "0")
    private Boolean gender;

    /**
     * 生日
     */
    @Schema(description = "生日", example = "2000-01-01")
    private LocalDate birthday;

    /**
     * 积分
     */
    @Schema(description = "积分", example = "200")
    private Integer credits;

    /**
     * 会员级别，0~9级,0代表未开通会员
     */
    @Schema(description = "会员级别，0~9级，0代表未开通", example = "0")
    private Boolean level;

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
