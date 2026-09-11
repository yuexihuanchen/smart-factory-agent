package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.common.response.PageResult;
import com.smartfactory.dto.DeviceQueryRequest;
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
    public PageResult<Device> findPage(DeviceQueryRequest request) {

        int page = request.getPage();
        int size = request.getSize();

        int offset = (page - 1) * size;

        List<Device> records = deviceMapper.findPage(
                offset,
                size,
                request.getKeyword(),
                request.getDeviceType(),
                request.getStatus(),
                request.getProtocol()
        );

        long total = deviceMapper.count(
                request.getKeyword(),
                request.getDeviceType(),
                request.getStatus(),
                request.getProtocol()
        );

        return new PageResult<>(
                records,
                page,
                size,
                total
        );
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
        return deviceMapper.findById(device.getId());
    }

    @Override
    public Device update(Device device) {
        int rows = deviceMapper.update(device);

        if (rows == 0) {
            throw new BusinessException(40401, "设备不存在");
        }

        Device updated = deviceMapper.findById(device.getId());

        if (updated == null) {
            throw new BusinessException(40401, "设备不存在");
        }

        return updated;
    }

    @Override
    public void deleteById(Long id) {
        int rows = deviceMapper.deleteById(id);

        if (rows == 0) {
            throw new BusinessException(40401, "设备不存在");
        }
    }
}