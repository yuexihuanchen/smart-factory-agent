package com.smartfactory.controller;

import com.smartfactory.common.response.Result;
import com.smartfactory.entity.SysPermission;
import com.smartfactory.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Validated
@Tag(name = "权限管理", description = "系统权限查询接口")
public class PermissionController {

    private final PermissionService permissionService;

    @PreAuthorize("hasAuthority('permission:read')")
    @GetMapping
    @Operation(summary = "查询权限列表")
    public Result<List<SysPermission>> findAll() {
        return Result.success(permissionService.findAll());
    }

    @PreAuthorize("hasAuthority('permission:read')")
    @GetMapping("/{id}")
    @Operation(summary = "查询权限详情")
    public Result<SysPermission> findById(
            @PathVariable
            @Positive(message = "权限ID必须大于0")
            Long id) {
        return Result.success(permissionService.findById(id));
    }
}
