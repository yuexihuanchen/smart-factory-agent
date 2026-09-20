package com.smartfactory.mq;

import com.smartfactory.entity.OutboxEvent;
import com.smartfactory.mapper.OutboxEventMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    private final OutboxEventMapper outboxEventMapper =
            mock(OutboxEventMapper.class);

    private final RabbitTemplate rabbitTemplate =
            mock(RabbitTemplate.class);

    private final JsonMapper jsonMapper =
            JsonMapper.builder().build();

    @Test
    void nackKeepsPendingAndUsesOutboxIdAsCorrelationId() {

        OutboxEvent outbox = outbox(10001L);
        AtomicReference<String> correlationId =
                new AtomicReference<>();

        when(outboxEventMapper.findPending(100))
                .thenReturn(List.of(outbox));
        when(outboxEventMapper.markPublishFailure(
                eq(10001L),
                anyString()
        )).thenReturn(1);

        doAnswer(invocation -> {
            CorrelationData correlation =
                    invocation.getArgument(4);

            correlationId.set(correlation.getId());
            correlation.getFuture().complete(
                    new CorrelationData.Confirm(
                            false,
                            "broker rejected"
                    )
            );
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(DeviceAlarmEventMessage.class),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );

        OutboxPublisher publisher = new OutboxPublisher(
                outboxEventMapper,
                rabbitTemplate,
                jsonMapper
        );

        List<OutboxPublisher.PublishResult> results =
                publisher.publishPending();

        assertThat(correlationId.get()).isEqualTo("10001");
        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.outboxId()).isEqualTo(10001L);
                    assertThat(result.outcome())
                            .isEqualTo(
                                    OutboxPublisher.Outcome.NACK
                            );
                    assertThat(result.confirmed()).isFalse();
                    assertThat(result.returned()).isFalse();
                });
        verify(outboxEventMapper).markPublishFailure(
                eq(10001L),
                contains("broker rejected")
        );
        verify(outboxEventMapper, never())
                .markSent(any(), any());
    }

    private OutboxEvent outbox(Long id) {

        DeviceAlarmEventMessage message =
                new DeviceAlarmEventMessage();
        message.setSource("gateway-A");
        message.setEventId("EVT-NACK-001");
        message.setDeviceId(1L);
        message.setAlarmCode("TEMP_HIGH");
        message.setAlarmType("TEMPERATURE");
        message.setAlarmLevel("CRITICAL");
        message.setTitle("temperature high");
        message.setMessage("temperature high");
        message.setOccurredAt(
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        message.setPayload("{\"temperature\":95}");

        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setSource(message.getSource());
        event.setEventId(message.getEventId());
        event.setExchange("device.exchange");
        event.setRoutingKey("device.alarm");
        event.setPayload(
                jsonMapper.writeValueAsString(message)
        );
        event.setStatus("PENDING");
        event.setRetryCount(0);
        return event;
    }
}
