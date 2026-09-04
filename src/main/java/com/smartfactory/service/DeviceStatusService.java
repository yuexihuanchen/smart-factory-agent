package com.smartfactory.service;

public interface DeviceStatusService {
    // → 查询状态
    String getStatus(Long deviceId);

    // → 修改状态
    void updateStatus(Long deviceId, String status);
}