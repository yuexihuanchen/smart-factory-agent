package com.smartfactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建告警请求")
public class AlarmCreateRequest {

    @Schema(description = "设备ID", example = "3")
    @NotNull(message = "设备ID不能为空")
    @Positive(message = "设备ID必须大于0")
    private Long deviceId;

    @Schema(description = "告警编码", example = "TEMP_HIGH")
    @NotBlank(message = "告警编码不能为空")
    @Size(max = 64, message = "告警编码长度不能超过64个字符")
    private String alarmCode;

    @Schema(description = "告警类型", example = "TEMPERATURE")
    @Size(max = 64, message = "告警类型长度不能超过64个字符")
    private String alarmType;

    @Schema(description = "告警等级", example = "CRITICAL")
    @NotBlank(message = "告警等级不能为空")
    @Size(max = 20, message = "告警等级长度不能超过20个字符")
    private String alarmLevel;

    @Schema(description = "告警标题", example = "设备温度过高")
    @Size(max = 255, message = "告警标题长度不能超过255个字符")
    private String title;

    @Schema(description = "告警消息", example = "设备3温度持续超过安全阈值")
    private String message;

    @Schema(
            description = "故障实际发生时间；为空时使用数据库当前时间",
            example = "2026-09-15T15:30:00"
    )
    private LocalDateTime occurredAt;
}
