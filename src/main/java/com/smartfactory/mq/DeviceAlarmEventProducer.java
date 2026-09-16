package com.smartfactory.mq;

import com.smartfactory.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceAlarmEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public DeviceAlarmCorrelationData send(
            DeviceAlarmEventMessage message) {

        return send(
                RabbitMQConfig.DEVICE_EXCHANGE,
                RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY,
                message
        );
    }

    public DeviceAlarmCorrelationData send(
            String exchange,
            String routingKey,
            DeviceAlarmEventMessage message) {

        DeviceAlarmCorrelationData correlationData =
                new DeviceAlarmCorrelationData(
                        message,
                        exchange,
                        routingKey
                );

        MessagePostProcessor messagePostProcessor = amqpMessage -> {
            amqpMessage.getMessageProperties().setHeader(
                    "alarm.source",
                    message.getSource()
            );
            amqpMessage.getMessageProperties().setHeader(
                    "alarm.eventId",
                    message.getEventId()
            );
            amqpMessage.getMessageProperties().setHeader(
                    "alarm.correlationId",
                    correlationData.getId()
            );
            return amqpMessage;
        };

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                message,
                messagePostProcessor,
                correlationData
        );

        return correlationData;
    }
}
