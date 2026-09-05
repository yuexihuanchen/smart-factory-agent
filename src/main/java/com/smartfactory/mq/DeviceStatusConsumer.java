package com.smartfactory.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeviceStatusConsumer {

    @RabbitListener(queues = "device.status.queue")
    public void consume(DeviceStatusMessage message) {

        log.info(
                "收到设备状态事件: deviceId={}, deviceCode={}, status={}",
                message.getDeviceId(),
                message.getDeviceCode(),
                message.getStatus()
        );
    }
}