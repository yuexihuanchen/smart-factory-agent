package com.smartfactory.service;

import com.smartfactory.common.response.PageResult;
import com.smartfactory.dto.DeviceQueryRequest;
import com.smartfactory.entity.Device;

public interface DeviceService {

    PageResult<Device> findPage(DeviceQueryRequest request);

    Device findById(Long id);

    Device create(Device device);

    Device update(Device device);

    void deleteById(Long id);
}