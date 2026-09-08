package com.smartfactory.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysPermission {

    private Long id;

    private String permissionName;

    private String permissionCode;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}