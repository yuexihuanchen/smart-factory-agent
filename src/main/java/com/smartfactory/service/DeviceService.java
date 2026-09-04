package com.smartfactory.service;

import com.smartfactory.entity.Device;

import java.util.List;

public interface DeviceService {

    List<Device> findAll();

    Device findById(Long id);

    Device create(Device device);

    Device update(Device device);

    void deleteById(Long id);
}
