package com.smartfactory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "刷新令牌结果")
public class RefreshTokenResponse {

    @Schema(description = "新的访问令牌")
    private String accessToken;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "访问令牌有效期（秒）")
    private long expiresInSeconds;

    @Schema(description = "新的刷新令牌")
    private String refreshToken;

    @Schema(description = "刷新令牌有效期（秒）")
    private long refreshExpiresInSeconds;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "权限列表")
    private List<String> authorities;
}