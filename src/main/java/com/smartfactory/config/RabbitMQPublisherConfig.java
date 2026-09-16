package com.smartfactory.config;

import com.smartfactory.mq.DeviceAlarmCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQPublisherConfig {

    @Bean
    public RabbitTemplateCustomizer alarmPublisherReliabilityCustomizer() {

        return template -> {
            template.setMandatory(true);

            template.setConfirmCallback((correlationData, ack, cause) -> {
                if (correlationData
                        instanceof DeviceAlarmCorrelationData alarmData) {
                    if (ack) {
                        log.info(
                                "RabbitMQ publish confirmed: exchange={}, routingKey={}, source={}, eventId={}, correlationId={}",
                                alarmData.getExchange(),
                                alarmData.getRoutingKey(),
                                alarmData.getSource(),
                                alarmData.getEventId(),
                                alarmData.getId()
                        );
                    } else {
                        log.warn(
                                "RabbitMQ publish rejected: exchange={}, routingKey={}, source={}, eventId={}, correlationId={}, cause={}",
                                alarmData.getExchange(),
                                alarmData.getRoutingKey(),
                                alarmData.getSource(),
                                alarmData.getEventId(),
                                alarmData.getId(),
                                cause
                        );
                    }
                }
            });

            template.setReturnsCallback(this::logReturnedMessage);
        };
    }

    private void logReturnedMessage(ReturnedMessage returned) {

        Object sourceHeader = returned.getMessage()
                .getMessageProperties()
                .getHeader("alarm.source");
        Object eventIdHeader = returned.getMessage()
                .getMessageProperties()
                .getHeader("alarm.eventId");

        String source = sourceHeader == null
                ? "unknown"
                : sourceHeader.toString();
        String eventId = eventIdHeader == null
                ? "unknown"
                : eventIdHeader.toString();

        log.warn(
                "RabbitMQ publish returned: exchange={}, routingKey={}, source={}, eventId={}, replyCode={}, replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                source,
                eventId,
                returned.getReplyCode(),
                returned.getReplyText()
        );
    }
}
