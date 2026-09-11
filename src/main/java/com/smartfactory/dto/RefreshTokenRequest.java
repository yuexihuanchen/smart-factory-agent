package com.smartfactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "刷新令牌请求")
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken不能为空")
    @Schema(description = "刷新令牌")
    private String refreshToken;
}