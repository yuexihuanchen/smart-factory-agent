package com.smartfactory.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DEVICE_EXCHANGE = "device.exchange";

    public static final String DEVICE_STATUS_QUEUE = "device.status.queue";

    public static final String DEVICE_STATUS_ROUTING_KEY = "device.status";

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
            Queue deviceStatusQueue,
            DirectExchange deviceExchange) {

        return BindingBuilder
                .bind(deviceStatusQueue)
                .to(deviceExchange)
                .with(DEVICE_STATUS_ROUTING_KEY);
    }
}