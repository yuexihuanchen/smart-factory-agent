package com.smartfactory.controller;

import com.smartfactory.entity.Device;
import com.smartfactory.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public List<Device> findAll() {
        return deviceService.findAll();
    }

    @GetMapping("/{id}")
    public Device findById(@PathVariable Long id) {
        return deviceService.findById(id);
    }

    @PostMapping
    public Device create(@RequestBody Device device) {
        return deviceService.create(device);
    }

    @PutMapping("/{id}")
    public Device update(
            @PathVariable Long id,
            @RequestBody Device device) {

        device.setId(id);

        return deviceService.update(device);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {

        deviceService.deleteById(id);

        return "删除成功";
    }
}