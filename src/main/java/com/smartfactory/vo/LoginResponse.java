package com.smartfactory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "登录结果")
public class LoginResponse {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "有效期（秒）")
    private long expiresInSeconds;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "权限列表")
    private List<String> authorities;
}
