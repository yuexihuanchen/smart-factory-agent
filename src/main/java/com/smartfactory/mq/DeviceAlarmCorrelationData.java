package com.smartfactory.mq;

import lombok.Getter;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.util.UUID;

@Getter
public class DeviceAlarmCorrelationData extends CorrelationData {

    private final String source;

    private final String eventId;

    private final String exchange;

    private final String routingKey;

    public DeviceAlarmCorrelationData(
            DeviceAlarmEventMessage message,
            String exchange,
            String routingKey) {
        super(UUID.randomUUID().toString());

        this.source = message.getSource();
        this.eventId = message.getEventId();
        this.exchange = exchange;
        this.routingKey = routingKey;
    }
}
