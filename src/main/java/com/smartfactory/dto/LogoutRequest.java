package com.smartfactory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {

    @NotBlank(message = "Refresh Token 不能为空")
    private String refreshToken;
}