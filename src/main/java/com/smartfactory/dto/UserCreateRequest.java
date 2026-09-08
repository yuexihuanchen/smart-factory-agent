package com.smartfactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建用户请求")
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(
            min = 3,
            max = 64,
            message = "用户名长度必须在3到64个字符之间"
    )
    @Schema(
            description = "用户名",
            example = "maintenance"
    )
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(
            min = 6,
            max = 64,
            message = "密码长度必须在6到64个字符之间"
    )
    @Schema(
            description = "登录密码",
            example = "123456"
    )
    private String password;

    @Size(
            max = 64,
            message = "昵称长度不能超过64个字符"
    )
    @Schema(
            description = "用户昵称",
            example = "维修人员"
    )
    private String nickname;
}