package com.smartfactory.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String DEVICE_EXCHANGE = "device.exchange";

    public static final String DEVICE_STATUS_QUEUE = "device.status.queue";

    public static final String DEVICE_STATUS_ROUTING_KEY = "device.status";

    public static final String DEVICE_ALARM_QUEUE = "device.alarm.queue";

    public static final String DEVICE_ALARM_ROUTING_KEY = "device.alarm";

    public static final String DEVICE_ALARM_DLX = "device.alarm.dlx";

    public static final String DEVICE_ALARM_DLQ = "device.alarm.dlq";

    public static final String DEVICE_ALARM_DLQ_ROUTING_KEY =
            "device.alarm.dlq";

    @Bean
    public DirectExchange deviceExchange() {
        return new DirectExchange(DEVICE_EXCHANGE);
    }

    @Bean
    public Queue deviceStatusQueue() {
        return new Queue(DEVICE_STATUS_QUEUE, true);
    }

    @Bean
    public Binding deviceStatusBinding(
            @Qualifier("deviceStatusQueue") Queue deviceStatusQueue,
            @Qualifier("deviceExchange") DirectExchange deviceExchange) {

        return BindingBuilder
                .bind(deviceStatusQueue)
                .to(deviceExchange)
                .with(DEVICE_STATUS_ROUTING_KEY);
    }

    @Bean
    public Queue deviceAlarmQueue() {

        Map<String, Object> arguments = new HashMap<>();

        // Retry 耗尽且消息被 reject 时，由 RabbitMQ 原生死信机制转发。
        arguments.put(
                "x-dead-letter-exchange",
                DEVICE_ALARM_DLX
        );
        arguments.put(
                "x-dead-letter-routing-key",
                DEVICE_ALARM_DLQ_ROUTING_KEY
        );

        return new Queue(
                DEVICE_ALARM_QUEUE,
                true,
                false,
                false,
                arguments
        );
    }

    @Bean
    public Binding deviceAlarmBinding(
            @Qualifier("deviceAlarmQueue") Queue deviceAlarmQueue,
            @Qualifier("deviceExchange") DirectExchange deviceExchange) {

        return BindingBuilder
                .bind(deviceAlarmQueue)
                .to(deviceExchange)
                .with(DEVICE_ALARM_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deviceAlarmDlx() {
        return new DirectExchange(DEVICE_ALARM_DLX);
    }

    @Bean
    public Queue deviceAlarmDlq() {
        return new Queue(DEVICE_ALARM_DLQ, true);
    }

    @Bean
    public Binding deviceAlarmDlqBinding(
            @Qualifier("deviceAlarmDlq") Queue deviceAlarmDlq,
            @Qualifier("deviceAlarmDlx") DirectExchange deviceAlarmDlx) {

        return BindingBuilder
                .bind(deviceAlarmDlq)
                .to(deviceAlarmDlx)
                .with(DEVICE_ALARM_DLQ_ROUTING_KEY);
    }
}
