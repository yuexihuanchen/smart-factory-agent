package com.smartfactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "告警分页查询请求")
public class AlarmQueryRequest {

    @Schema(description = "页码，从1开始", example = "1")
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    @Schema(description = "每页数量，最大100", example = "20")
    @Min(value = 1, message = "每页数量必须大于等于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 20;

    @Schema(description = "设备ID", example = "3")
    @Positive(message = "设备ID必须大于0")
    private Long deviceId;

    @Schema(description = "告警编码", example = "TEMP_HIGH")
    private String alarmCode;

    @Schema(description = "告警等级", example = "CRITICAL")
    private String alarmLevel;

    @Schema(description = "告警状态", example = "ACTIVE")
    private String status;

    @Schema(description = "关键字，可匹配告警编码、标题或消息")
    private String keyword;

    @Schema(description = "最后发生时间开始值")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @Schema(description = "最后发生时间结束值")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}
