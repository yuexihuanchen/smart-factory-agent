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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
