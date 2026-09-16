package com.smartfactory.config;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.common.exception.TransientAlarmException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;

@Configuration
public class RabbitMQListenerConfig {

    @Value("${smart-factory.rabbitmq.alarm.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${smart-factory.rabbitmq.alarm.retry.initial-interval-ms:1000}")
    private long initialIntervalMs;

    @Value("${smart-factory.rabbitmq.alarm.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${smart-factory.rabbitmq.alarm.retry.max-interval-ms:5000}")
    private long maxIntervalMs;

    @Bean
    public SimpleRabbitListenerContainerFactory
    alarmRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(
                RetryInterceptorBuilder
                        .stateless()
                        .configureRetryPolicy(retryPolicy -> retryPolicy
                                .maxRetries(
                                        Math.max(0, maxAttempts - 1)
                                )
                                .includes(
                                        TransientAlarmException.class,
                                        TransientDataAccessException.class
                                )
                                .excludes(BusinessException.class)
                        )
                        .backOffOptions(
                                initialIntervalMs,
                                multiplier,
                                maxIntervalMs
                        )
                        .recoverer(
                                new RejectAndDontRequeueRecoverer()
                        )
                        .build()
        );

        return factory;
    }
}
