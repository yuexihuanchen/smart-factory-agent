package com.smartfactory.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Device {

    private Long id;

    private String deviceCode;

    private String deviceName;

    private String deviceType;

    private String location;

    private String ipAddress;

    private Integer port;

    private String protocol;

    private String status;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // getter / setter
}