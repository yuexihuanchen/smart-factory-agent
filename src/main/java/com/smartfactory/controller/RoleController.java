package com.smartfactory.controller;

import com.smartfactory.common.response.Result;
import com.smartfactory.dto.RoleCreateRequest;
import com.smartfactory.dto.RolePermissionAssignRequest;
import com.smartfactory.dto.RoleUpdateRequest;
import com.smartfactory.service.RoleService;
import com.smartfactory.vo.RoleVO;
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
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Validated
@Tag(name = "角色管理", description = "系统角色与权限分配管理接口")
public class RoleController {

    private final RoleService roleService;

    @PreAuthorize("hasAuthority('role:read')")
    @GetMapping
    @Operation(summary = "查询角色列表")
    public Result<List<RoleVO>> findAll() {
        return Result.success(roleService.findAll());
    }

    @PreAuthorize("hasAuthority('role:read')")
    @GetMapping("/{id}")
    @Operation(summary = "查询角色详情")
    public Result<RoleVO> findById(
            @PathVariable
            @Positive(message = "角色ID必须大于0")
            Long id) {
        return Result.success(roleService.findById(id));
    }

    @PreAuthorize("hasAuthority('role:create')")
    @PostMapping
    @Operation(summary = "创建角色")
    public Result<RoleVO> create(
            @Valid
            @RequestBody
            RoleCreateRequest request) {
        return Result.success(roleService.create(request));
    }

    @PreAuthorize("hasAuthority('role:update')")
    @PutMapping("/{id}")
    @Operation(summary = "修改角色")
    public Result<RoleVO> update(
            @PathVariable
            @Positive(message = "角色ID必须大于0")
            Long id,

            @Valid
            @RequestBody
            RoleUpdateRequest request) {
        return Result.success(roleService.update(id, request));
    }

    @PreAuthorize("hasAuthority('role:delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    public Result<Void> delete(
            @PathVariable
            @Positive(message = "角色ID必须大于0")
            Long id) {
        roleService.deleteById(id);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('role:update')")
    @PutMapping("/{id}/permissions")
    @Operation(summary = "分配角色权限")
    public Result<RoleVO> assignPermissions(
            @PathVariable
            @Positive(message = "角色ID必须大于0")
            Long id,

            @Valid
            @RequestBody
            RolePermissionAssignRequest request) {
        return Result.success(
                roleService.assignPermissions(
                        id,
                        request.getPermissionIds()
                )
        );
    }
}
