package com.smartfactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "设备分页查询请求")
public class DeviceQueryRequest {

    @Schema(description = "页码，从1开始", example = "1")
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    @Schema(description = "每页数量，最大100", example = "20")
    @Min(value = 1, message = "每页数量必须大于等于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 20;

    @Schema(description = "关键字，可匹配设备编号或设备名称", example = "PLC")
    private String keyword;

    @Schema(description = "设备类型", example = "PLC")
    private String deviceType;

    @Schema(description = "设备状态", example = "RUNNING")
    private String status;

    @Schema(description = "通信协议", example = "MODBUS_TCP")
    private String protocol;
}