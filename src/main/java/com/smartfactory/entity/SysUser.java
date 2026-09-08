package com.smartfactory.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {

    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}