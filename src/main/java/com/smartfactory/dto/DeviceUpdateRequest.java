package com.smartfactory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class DeviceUpdateRequest {

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 128, message = "设备名称长度不能超过128个字符")
    private String deviceName;

    @NotBlank(message = "设备类型不能为空")
    @Size(max = 64, message = "设备类型长度不能超过64个字符")
    private String deviceType;

    private String ipAddress;

    private Integer port;

    private String protocol;

    private String status;

    private String description;
}