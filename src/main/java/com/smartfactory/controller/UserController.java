package com.smartfactory.controller;

import com.smartfactory.common.response.Result;
import com.smartfactory.dto.UserCreateRequest;
import com.smartfactory.dto.UserRoleAssignRequest;
import com.smartfactory.dto.UserUpdateRequest;
import com.smartfactory.service.UserService;
import com.smartfactory.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户管理", description = "系统用户与角色分配管理接口")
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping
    @Operation(summary = "查询用户列表")
    public Result<List<UserVO>> findAll() {
        return Result.success(userService.findAll());
    }

    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情")
    public Result<UserVO> findById(
            @PathVariable
            @Positive(message = "用户ID必须大于0")
            Long id) {
        return Result.success(userService.findById(id));
    }

    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping
    @Operation(
            summary = "创建用户",
            description = "密码使用 BCrypt 加密后保存"
    )
    public Result<UserVO> create(
            @Valid
            @RequestBody
            UserCreateRequest request) {
        return Result.success(userService.create(request));
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/{id}")
    @Operation(summary = "修改用户")
    public Result<UserVO> update(
            @PathVariable
            @Positive(message = "用户ID必须大于0")
            Long id,

            @Valid
            @RequestBody
            UserUpdateRequest request) {
        return Result.success(userService.update(id, request));
    }

    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public Result<Void> delete(
            @PathVariable
            @Positive(message = "用户ID必须大于0")
            Long id) {
        userService.deleteById(id);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/{id}/roles")
    @Operation(summary = "分配用户角色")
    public Result<UserVO> assignRoles(
            @PathVariable
            @Positive(message = "用户ID必须大于0")
            Long id,

            @Valid
            @RequestBody
            UserRoleAssignRequest request) {
        return Result.success(
                userService.assignRoles(id, request.getRoleIds())
        );
    }
}
