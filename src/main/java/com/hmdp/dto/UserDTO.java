package com.hmdp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户展示信息")
public class UserDTO {
    @Schema(description = "用户id", example = "1")
    private Long id;
    @Schema(description = "用户昵称", example = "小鱼同学")
    private String nickName;
    @Schema(description = "用户头像", example = "https://example.com/icon.png")
    private String icon;
}
