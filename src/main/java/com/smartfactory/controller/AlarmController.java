package com.smartfactory.controller;

import com.smartfactory.common.response.PageResult;
import com.smartfactory.common.response.Result;
import com.smartfactory.dto.AlarmCreateRequest;
import com.smartfactory.dto.AlarmQueryRequest;
import com.smartfactory.entity.Alarm;
import com.smartfactory.service.AlarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
@Validated
@Tag(name = "设备告警", description = "设备告警创建、查询与处理接口")
public class AlarmController {

    private final AlarmService alarmService;

    @PreAuthorize("hasAuthority('alarm:create')")
    @PostMapping
    @Operation(
            summary = "创建告警",
            description = "重复发生相同未恢复故障时聚合到现有告警"
    )
    public Result<Alarm> create(
            @Valid
            @RequestBody
            AlarmCreateRequest request) {

        return Result.success(alarmService.create(request));
    }

    @PreAuthorize("hasAuthority('alarm:read')")
    @GetMapping
    @Operation(summary = "分页查询告警")
    public Result<PageResult<Alarm>> findPage(
            @Valid AlarmQueryRequest request) {

        return Result.success(alarmService.findPage(request));
    }

    @PreAuthorize("hasAuthority('alarm:read')")
    @GetMapping("/{id}")
    @Operation(summary = "查询告警详情")
    public Result<Alarm> findById(
            @PathVariable
            @Positive(message = "告警ID必须大于0")
            Long id) {

        return Result.success(alarmService.findById(id));
    }

    @PreAuthorize("hasAuthority('alarm:ack')")
    @PostMapping("/{id}/ack")
    @Operation(summary = "确认告警")
    public Result<Alarm> acknowledge(
            @PathVariable
            @Positive(message = "告警ID必须大于0")
            Long id,

            Authentication authentication) {

        return Result.success(
                alarmService.acknowledge(
                        id,
                        authentication.getName()
                )
        );
    }

    @PreAuthorize("hasAuthority('alarm:resolve')")
    @PostMapping("/{id}/resolve")
    @Operation(summary = "恢复告警")
    public Result<Alarm> resolve(
            @PathVariable
            @Positive(message = "告警ID必须大于0")
            Long id) {

        return Result.success(alarmService.resolve(id));
    }
}
