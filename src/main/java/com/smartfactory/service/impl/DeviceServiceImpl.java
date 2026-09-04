package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
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
        Device device = deviceMapper.findById(id);

        if (device == null) {
            throw new BusinessException(40401, "设备不存在");
        }

        return device;
    }

    @Override
    public Device create(Device device) {
        deviceMapper.insert(device);
        return device;
    }

    @Override
    public Device update(Device device) {
        int rows = deviceMapper.update(device);

        if (rows == 0) {
            throw new BusinessException(40401, "设备不存在");
        }

        return device;
    }

    @Override
    public void deleteById(Long id) {
        int rows = deviceMapper.deleteById(id);

        if (rows == 0) {
            throw new BusinessException(40401, "设备不存在");
        }
    }
}