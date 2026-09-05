package com.smartfactory.mq;

import com.smartfactory.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceStatusProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(DeviceStatusMessage message) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DEVICE_EXCHANGE,
                RabbitMQConfig.DEVICE_STATUS_ROUTING_KEY,
                message
        );
    }
}