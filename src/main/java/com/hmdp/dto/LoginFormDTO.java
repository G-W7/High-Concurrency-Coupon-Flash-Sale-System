package com.hmdp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录表单参数")
public class LoginFormDTO {
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String phone;
    @Schema(description = "验证码", example = "1234")
    private String code;
    @Schema(description = "密码", example = "123456")
    private String password;
}
