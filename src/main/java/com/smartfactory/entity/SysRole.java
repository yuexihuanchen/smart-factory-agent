package com.smartfactory.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysRole {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}