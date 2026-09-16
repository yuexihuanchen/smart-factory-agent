package com.smartfactory.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQAlarmConfigurationTest {

    @Test
    void declaresAlarmTopologyWithoutBreakingDeviceStatusTopology() {

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(
                             RabbitMQConfig.class
                     )) {

            DirectExchange exchange = context.getBean(
                    "deviceExchange",
                    DirectExchange.class
            );
            Queue statusQueue = context.getBean(
                    "deviceStatusQueue",
                    Queue.class
            );
            Queue alarmQueue = context.getBean(
                    "deviceAlarmQueue",
                    Queue.class
            );
            DirectExchange alarmDlx = context.getBean(
                    "deviceAlarmDlx",
                    DirectExchange.class
            );
            Queue alarmDlq = context.getBean(
                    "deviceAlarmDlq",
                    Queue.class
            );
            Binding statusBinding = context.getBean(
                    "deviceStatusBinding",
                    Binding.class
            );
            Binding alarmBinding = context.getBean(
                    "deviceAlarmBinding",
                    Binding.class
            );
            Binding alarmDlqBinding = context.getBean(
                    "deviceAlarmDlqBinding",
                    Binding.class
            );

            assertThat(exchange.getName())
                    .isEqualTo(RabbitMQConfig.DEVICE_EXCHANGE);
            assertThat(exchange.isDurable()).isTrue();

            assertThat(statusQueue.getName())
                    .isEqualTo(RabbitMQConfig.DEVICE_STATUS_QUEUE);
            assertThat(statusQueue.isDurable()).isTrue();
            assertThat(statusBinding.getDestination())
                    .isEqualTo(RabbitMQConfig.DEVICE_STATUS_QUEUE);
            assertThat(statusBinding.getRoutingKey())
                    .isEqualTo(RabbitMQConfig.DEVICE_STATUS_ROUTING_KEY);

            assertThat(alarmQueue.getName())
                    .isEqualTo(RabbitMQConfig.DEVICE_ALARM_QUEUE);
            assertThat(alarmQueue.isDurable()).isTrue();
            assertThat(alarmQueue.getArguments())
                    .containsEntry(
                            "x-dead-letter-exchange",
                            RabbitMQConfig.DEVICE_ALARM_DLX
                    )
                    .containsEntry(
                            "x-dead-letter-routing-key",
                            RabbitMQConfig.DEVICE_ALARM_DLQ_ROUTING_KEY
                    );
            assertThat(alarmBinding.getDestination())
                    .isEqualTo(RabbitMQConfig.DEVICE_ALARM_QUEUE);
            assertThat(alarmBinding.getExchange())
                    .isEqualTo(RabbitMQConfig.DEVICE_EXCHANGE);
            assertThat(alarmBinding.getRoutingKey())
                    .isEqualTo(RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY);

            assertThat(alarmDlx.getName())
                    .isEqualTo(RabbitMQConfig.DEVICE_ALARM_DLX);
            assertThat(alarmDlx.isDurable()).isTrue();

            assertThat(alarmDlq.getName())
                    .isEqualTo(RabbitMQConfig.DEVICE_ALARM_DLQ);
            assertThat(alarmDlq.isDurable()).isTrue();

            assertThat(alarmDlqBinding.getDestination())
                    .isEqualTo(RabbitMQConfig.DEVICE_ALARM_DLQ);
            assertThat(alarmDlqBinding.getExchange())
                    .isEqualTo(RabbitMQConfig.DEVICE_ALARM_DLX);
            assertThat(alarmDlqBinding.getRoutingKey())
                    .isEqualTo(
                            RabbitMQConfig.DEVICE_ALARM_DLQ_ROUTING_KEY
                    );
        }
    }
}
