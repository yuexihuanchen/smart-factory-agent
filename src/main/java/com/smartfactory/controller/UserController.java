package com.smartfactory.controller;

import com.smartfactory.common.response.Result;
import com.smartfactory.dto.UserCreateRequest;
import com.smartfactory.service.UserService;
import com.smartfactory.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(
        name = "用户管理",
        description = "系统用户管理接口"
)
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     */
    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping
    @Operation(
            summary = "创建用户",
            description = "创建系统用户，密码使用 BCrypt 加密后保存"
    )
    public Result<UserVO> create(
            @Valid
            @RequestBody
            UserCreateRequest request) {

        return Result.success(
                userService.create(request)
        );
    }
}