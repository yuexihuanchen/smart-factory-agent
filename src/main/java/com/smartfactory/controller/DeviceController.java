package com.smartfactory.controller;

import com.smartfactory.common.response.Result;
import com.smartfactory.dto.DeviceCreateRequest;
import com.smartfactory.dto.DeviceUpdateRequest;
import com.smartfactory.entity.Device;
import com.smartfactory.service.DeviceService;
import com.smartfactory.service.DeviceStatusService;
import com.smartfactory.common.exception.BusinessException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import com.smartfactory.service.DeviceStatusService;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "设备管理",
        description = "工业设备基础信息管理接口"
)
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceStatusService deviceStatusService;

    /**
     * 查询设备列表
     */
    @GetMapping
    @Operation(
            summary = "查询设备列表",
            description = "查询系统中的所有设备"
    )
    public Result<List<Device>> findAll() {

        return Result.success(
                deviceService.findAll()
        );
    }

    /**
     * 查询设备详情
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "查询设备详情",
            description = "根据设备ID查询设备详细信息"
    )
    public Result<Device> findById(
            @PathVariable
            @Positive(message = "设备ID必须大于0")
            Long id) {

        return Result.success(
                deviceService.findById(id)
        );
    }


    @GetMapping("/{id}/status")
    @Operation(summary = "查询设备状态")
    public Result<String> getStatus(
        @Parameter(description = "设备ID", example = "1")
        @PathVariable
        @Positive
        Long id) {

    String status = deviceStatusService.getStatus(id);

    if (status == null) {
        throw new BusinessException(40401, "设备不存在");
    }

    return Result.success(status);
}

    /**
     * 创建设备
     */
    @PostMapping
    @Operation(
            summary = "创建设备",
            description = "新增一个工业设备"
    )
   public Result<Device> create(
                @Valid @RequestBody DeviceCreateRequest request) {

    Device device = new Device();

    device.setDeviceCode(request.getDeviceCode());
    device.setDeviceName(request.getDeviceName());
    device.setDeviceType(request.getDeviceType());
    device.setLocation(request.getLocation());
    device.setIpAddress(request.getIpAddress());
    device.setPort(request.getPort());
    device.setProtocol(request.getProtocol());
    device.setStatus(request.getStatus());
    device.setDescription(request.getDescription());
    

    return Result.success(
            deviceService.create(device)
    );
}

    /**
     * 修改设备
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "修改设备",
            description = "根据设备ID修改设备信息"
    )
    public Result<Device> update(
            @PathVariable
            @Positive(message = "设备ID必须大于0")
            Long id,

            @Valid @RequestBody DeviceUpdateRequest request) {

        Device device = new Device();

        device.setId(id);
        device.setDeviceName(request.getDeviceName());
        device.setDeviceType(request.getDeviceType());
        device.setIpAddress(request.getIpAddress());
        device.setPort(request.getPort());
        device.setProtocol(request.getProtocol());
        device.setStatus(request.getStatus());
        device.setDescription(request.getDescription());
        device.setLocation(request.getLocation());

        return Result.success(
                deviceService.update(device)
        );
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "删除设备",
            description = "根据设备ID删除设备"
    )
    public Result<Void> delete(
            @PathVariable
            @Positive(message = "设备ID必须大于0")
            Long id) {

        deviceService.deleteById(id);

        return Result.success();
    }

    @PutMapping("/{id}/status")
@Operation(summary = "修改设备状态")
public Result<Void> updateStatus(
        @Parameter(description = "设备ID", example = "1")
        @PathVariable
        @Positive
        Long id,

        @RequestParam
        String status) {

    deviceStatusService.updateStatus(id, status);

    return Result.success();
}
}

