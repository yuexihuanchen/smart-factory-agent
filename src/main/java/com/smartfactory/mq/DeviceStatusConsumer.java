package com.smartfactory.mq;

import com.smartfactory.entity.DeviceStatusHistory;
import com.smartfactory.repository.DeviceStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceStatusConsumer {

    private final DeviceStatusHistoryRepository repository;

    @RabbitListener(queues = "device.status.queue")
    public void consume(DeviceStatusMessage message) {

        log.info(
                "收到设备状态事件: deviceId={}, deviceCode={}, oldStatus={}, newStatus={}",
                message.getDeviceId(),
                message.getDeviceCode(),
                message.getOldStatus(),
                message.getNewStatus()
        );

        DeviceStatusHistory history = new DeviceStatusHistory();

        history.setDeviceId(message.getDeviceId());
        history.setDeviceCode(message.getDeviceCode());
        history.setOldStatus(message.getOldStatus());
        history.setNewStatus(message.getNewStatus());
        history.setTimestamp(LocalDateTime.now());

        repository.save(history);

        log.info(
                "设备状态历史已保存: deviceId={}, oldStatus={}, newStatus={}",
                message.getDeviceId(),
                message.getOldStatus(),
                message.getNewStatus()
        );
    }
}