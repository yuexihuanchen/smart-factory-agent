package com.smartfactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改用户请求")
public class UserUpdateRequest {

    @Size(max = 64, message = "昵称长度不能超过64个字符")
    @Schema(description = "昵称")
    private String nickname;

    @Pattern(
            regexp = "ENABLED|DISABLED",
            message = "用户状态只能是 ENABLED 或 DISABLED"
    )
    @Schema(description = "状态", example = "ENABLED")
    private String status;

    @Size(min = 6, max = 64, message = "密码长度必须在6到64个字符之间")
    @Schema(description = "新密码（可选）")
    private String password;
}
