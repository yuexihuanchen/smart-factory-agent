package com.smartfactory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
public class DeviceCreateRequest {

    @Schema(description = "设备编号", example = "PLC-003")
    @NotBlank(message = "设备编号不能为空")
    @Size(max = 64, message = "设备编号长度不能超过64个字符")
    private String deviceCode;

    @Schema(description = "设备名称", example = "三号产线PLC")    
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 128, message = "设备名称长度不能超过128个字符")
    private String deviceName;

    @Schema(description = "设备类型", example = "PLC")
    @NotBlank(message = "设备类型不能为空")
    @Size(max = 64, message = "设备类型长度不能超过64个字符")
    private String deviceType;

    private String ipAddress;

    private Integer port;

    private String protocol;

    private String status;

    private String description;

    @Schema(description = "设备位置", example = "三号车间")
    private String location;
}