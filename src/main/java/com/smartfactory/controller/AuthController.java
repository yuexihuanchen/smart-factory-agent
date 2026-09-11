package com.smartfactory.controller;

import com.smartfactory.common.response.Result;
import com.smartfactory.dto.LoginRequest;
import com.smartfactory.service.AuthService;
import com.smartfactory.vo.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smartfactory.dto.RefreshTokenRequest;
import com.smartfactory.vo.RefreshTokenResponse;
import com.smartfactory.dto.LogoutRequest;
import org.springframework.security.authentication.BadCredentialsException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "JWT 登录认证接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "账号密码登录",
            description = "登录成功后返回 JWT 访问令牌"
    )
    public Result<LoginResponse> login(
            @Valid
            @RequestBody
            LoginRequest request) {

        return Result.success(
                authService.login(request)
        );
    }

    @PostMapping("/refresh")
@Operation(
        summary = "刷新访问令牌",
        description = "使用 Refresh Token 换取新的 Access Token 和 Refresh Token"
)
public Result<RefreshTokenResponse> refresh(
        @Valid @RequestBody RefreshTokenRequest request) {

    return Result.success(
            authService.refresh(request)
    );
}

    @PostMapping("/logout")
@Operation(
    summary = "退出登录",
    description = "同时撤销当前 Access Token 和 Refresh Token"
)
public Result<Void> logout(
    @RequestHeader("Authorization") String authorization,
    @Valid @RequestBody LogoutRequest request) {

    if (authorization == null ||
        !authorization.startsWith("Bearer ")) {
        throw new BadCredentialsException("Authorization 请求头无效");
    }

    String accessToken = authorization.substring(7);

    authService.logout(
        accessToken,
        request.getRefreshToken()
    );

    return Result.success(null);
}
}
