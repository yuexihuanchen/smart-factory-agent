package com.smartfactory.service.impl;

import com.smartfactory.entity.Device;
import com.smartfactory.mapper.DeviceMapper;
import com.smartfactory.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;

    @Override
    public List<Device> findAll() {
        return deviceMapper.findAll();
    }

    @Override
    public Device findById(Long id) {
        return deviceMapper.findById(id);
    }

    @Override
    public Device create(Device device) {
        deviceMapper.insert(device);
        return device;
    }

    @Override
    public Device update(Device device) {
        deviceMapper.update(device);
        return device;
    }

    @Override
    public void deleteById(Long id) {
        deviceMapper.deleteById(id);
    }
}