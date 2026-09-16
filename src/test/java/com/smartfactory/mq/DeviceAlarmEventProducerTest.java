package com.smartfactory.mq;

import com.smartfactory.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeviceAlarmEventProducerTest {

    @Test
    void sendsAlarmEventWithExistingExchangeAndRoutingKey() {

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        DeviceAlarmEventProducer producer =
                new DeviceAlarmEventProducer(rabbitTemplate);
        DeviceAlarmEventMessage message = message();

        DeviceAlarmCorrelationData correlationData =
                producer.send(message);

        ArgumentCaptor<DeviceAlarmCorrelationData> captor =
                ArgumentCaptor.forClass(
                        DeviceAlarmCorrelationData.class
                );
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DEVICE_EXCHANGE),
                eq(RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY),
                eq(message),
                any(MessagePostProcessor.class),
                captor.capture()
        );

        assertThat(captor.getValue()).isSameAs(correlationData);
        assertThat(correlationData.getSource())
                .isEqualTo("gateway-A");
        assertThat(correlationData.getEventId())
                .isEqualTo("EVT-RMQ-001");
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
