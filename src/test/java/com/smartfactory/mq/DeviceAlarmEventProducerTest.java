package com.smartfactory.mq;

import com.smartfactory.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeviceAlarmEventProducerTest {

    @Test
    void sendsAlarmEventWithExistingExchangeAndRoutingKey() {

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        DeviceAlarmEventProducer producer =
                new DeviceAlarmEventProducer(rabbitTemplate);
        DeviceAlarmEventMessage message = message();

        producer.send(message);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.DEVICE_EXCHANGE,
                RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY,
                message
        );
    }

    private DeviceAlarmEventMessage message() {

        DeviceAlarmEventMessage message =
                new DeviceAlarmEventMessage();
        message.setSource("gateway-A");
        message.setEventId("EVT-RMQ-001");
        message.setDeviceId(3L);
        message.setAlarmCode("TEMP_HIGH");
        message.setAlarmType("TEMPERATURE");
        message.setAlarmLevel("CRITICAL");
        message.setTitle("设备温度过高");
        message.setMessage("温度超过阈值");
        message.setOccurredAt(
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        message.setPayload("{\"temperature\":95}");
        return message;
    }
}
