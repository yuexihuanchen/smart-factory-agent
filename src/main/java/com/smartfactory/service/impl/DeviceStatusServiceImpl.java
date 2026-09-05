package com.smartfactory.service.impl;

import com.smartfactory.entity.Device;
import com.smartfactory.mapper.DeviceMapper;
import com.smartfactory.service.DeviceStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import com.smartfactory.mq.DeviceStatusMessage;
import com.smartfactory.mq.DeviceStatusProducer;

@Service
@RequiredArgsConstructor
public class DeviceStatusServiceImpl implements DeviceStatusService {

    private final StringRedisTemplate redisTemplate;
    private final DeviceMapper deviceMapper;

    private static final long CACHE_MINUTES = 5;

    private final DeviceStatusProducer deviceStatusProducer;

    @Override
    public String getStatus(Long deviceId) {

        String key = "device:status:" + deviceId;

        // 1. 先查 Redis
        String status = redisTemplate.opsForValue().get(key);

        if (status != null) {
            return status;
        }

        // 2. Redis 没有，再查 MySQL
        Device device = deviceMapper.findById(deviceId);

        if (device == null) {
            return null;
        }

        status = device.getStatus();

        // 3. 写入 Redis，并设置 5 分钟 TTL
        if (status != null) {
            redisTemplate.opsForValue().set(
                    key,
                    status,
                    CACHE_MINUTES,
                    TimeUnit.MINUTES
            );
        }

        return status;
    }

    @Override
    public void updateStatus(Long deviceId, String status) {

        // 1. 查询设备是否存在
        Device device = deviceMapper.findById(deviceId);

        if (device == null) {
            return;
        }

        // 2. 修改 MySQL 中的状态
        device.setStatus(status);
        deviceMapper.update(device);

        // 3. 更新 Redis
        String key = "device:status:" + deviceId;

        redisTemplate.opsForValue().set(
                key,
                status,
                CACHE_MINUTES,
                TimeUnit.MINUTES
        );

        DeviceStatusMessage message = new DeviceStatusMessage(
        deviceId,
        device.getDeviceCode(),
        status
     );

        deviceStatusProducer.send(message);
    }
}