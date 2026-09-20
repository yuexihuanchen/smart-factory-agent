package com.smartfactory.mq;

import com.smartfactory.entity.OutboxEvent;
import com.smartfactory.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventMapper outboxEventMapper;

    private final RabbitTemplate rabbitTemplate;

    private final JsonMapper jsonMapper;

    @Value("${smart-factory.rabbitmq.outbox.batch-size:100}")
    private int batchSize = 100;

    @Value("${smart-factory.rabbitmq.outbox.confirm-timeout-ms:5000}")
    private long confirmTimeoutMs = 5000;

    @Value("${smart-factory.rabbitmq.outbox.lease-duration-ms:30000}")
    private long leaseDurationMs = 30000;

    private final String leaseOwner = UUID.randomUUID().toString();

    @Scheduled(
            fixedDelayString =
                    "${smart-factory.rabbitmq.outbox.poll-interval-ms:1000}"
    )
    public List<PublishResult> publishPending() {

        List<OutboxEvent> events =
                outboxEventMapper.findPending(Math.max(1, batchSize));
        List<PublishResult> results =
                new ArrayList<>(events.size());

        for (OutboxEvent event : events) {
            if (!tryClaim(event)) {
                log.debug(
                        "Outbox 已被其他实例认领，跳过: outboxId={}, source={}, eventId={}, leaseOwner={}",
                        event.getId(),
                        event.getSource(),
                        event.getEventId(),
                        leaseOwner
                );
                continue;
            }

            results.add(publishOne(event));
        }

        return results;
    }

    private boolean tryClaim(OutboxEvent event) {

        LocalDateTime leaseUntil = LocalDateTime.now().plusNanos(
                Math.max(1L, leaseDurationMs) * 1_000_000L
        );

        return outboxEventMapper.claim(
                event.getId(),
                leaseOwner,
                leaseUntil
        ) == 1;
    }

    private PublishResult publishOne(OutboxEvent event) {

        DeviceAlarmEventMessage message;

        try {
            message = jsonMapper.readValue(
                    event.getPayload(),
                    DeviceAlarmEventMessage.class
            );
        } catch (RuntimeException exception) {
            String error = exceptionMessage(exception);

            markPublishFailure(event, error);
            log.error(
                    "Outbox payload 解析失败: outboxId={}, source={}, eventId={}, exchange={}, routingKey={}, error={}",
                    event.getId(),
                    event.getSource(),
                    event.getEventId(),
                    event.getExchange(),
                    event.getRoutingKey(),
                    error
            );

            return failureResult(
                    event,
                    Outcome.PUBLISH_FAILED,
                    false,
                    false,
                    null,
                    null,
                    error
            );
        }

        CorrelationData correlationData =
                new CorrelationData(String.valueOf(event.getId()));

        try {
            MessagePostProcessor messagePostProcessor =
                    amqpMessage -> {
                        amqpMessage.getMessageProperties().setHeader(
                                "alarm.source",
                                event.getSource()
                        );
                        amqpMessage.getMessageProperties().setHeader(
                                "alarm.eventId",
                                event.getEventId()
                        );
                        amqpMessage.getMessageProperties().setHeader(
                                "alarm.correlationId",
                                correlationData.getId()
                        );
                        amqpMessage.getMessageProperties().setHeader(
                                "outbox.id",
                                event.getId()
                        );
                        return amqpMessage;
                    };

            rabbitTemplate.convertAndSend(
                    event.getExchange(),
                    event.getRoutingKey(),
                    message,
                    messagePostProcessor,
                    correlationData
            );

            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(
                            confirmTimeoutMs,
                            TimeUnit.MILLISECONDS
                    );

            ReturnedMessage returned = correlationData.getReturned();

            if (returned != null) {
                String error = "NO_ROUTE: replyCode="
                        + returned.getReplyCode()
                        + ", replyText="
                        + returned.getReplyText();

                markPublishFailure(event, error);
                log.warn(
                        "Outbox 消息不可路由: outboxId={}, source={}, eventId={}, exchange={}, routingKey={}, replyCode={}, replyText={}",
                        event.getId(),
                        event.getSource(),
                        event.getEventId(),
                        event.getExchange(),
                        event.getRoutingKey(),
                        returned.getReplyCode(),
                        returned.getReplyText()
                );

                return failureResult(
                        event,
                        Outcome.RETURNED,
                        true,
                        true,
                        returned.getReplyCode(),
                        returned.getReplyText(),
                        error
                );
            }

            if (!confirm.ack()) {
                String error = "NACK: " + confirm.reason();

                markPublishFailure(event, error);
                log.warn(
                        "Outbox 发布 NACK: outboxId={}, source={}, eventId={}, exchange={}, routingKey={}, reason={}",
                        event.getId(),
                        event.getSource(),
                        event.getEventId(),
                        event.getExchange(),
                        event.getRoutingKey(),
                        confirm.reason()
                );

                return failureResult(
                        event,
                        Outcome.NACK,
                        false,
                        false,
                        null,
                        null,
                        error
                );
            }

            int updated = outboxEventMapper.markSent(
                    event.getId(),
                    leaseOwner,
                    LocalDateTime.now()
            );

            if (updated != 1) {
                log.warn(
                        "Outbox 状态或 Lease Owner 已变化，跳过 SENT 更新: outboxId={}, source={}, eventId={}, exchange={}, routingKey={}, leaseOwner={}",
                        event.getId(),
                        event.getSource(),
                        event.getEventId(),
                        event.getExchange(),
                        event.getRoutingKey(),
                        leaseOwner
                );

                return failureResult(
                        event,
                        Outcome.STATE_CHANGED,
                        true,
                        false,
                        null,
                        null,
                        "Outbox 状态已变化"
                );
            }

            log.info(
                    "Outbox 发布成功并标记 SENT: outboxId={}, source={}, eventId={}, exchange={}, routingKey={}, leaseOwner={}",
                    event.getId(),
                    event.getSource(),
                    event.getEventId(),
                    event.getExchange(),
                    event.getRoutingKey(),
                    leaseOwner
            );

            return new PublishResult(
                    event.getId(),
                    Outcome.SENT,
                    true,
                    false,
                    null,
                    null,
                    null
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            return publishFailure(
                    event,
                    exception,
                    "Outbox 发布等待 Confirm 时被中断"
            );
        } catch (Exception exception) {
            return publishFailure(
                    event,
                    exception,
                    "Outbox 发布失败"
            );
        }
    }

    private PublishResult publishFailure(
            OutboxEvent event,
            Exception exception,
            String message) {

        String error = message + ": " + exceptionMessage(exception);

        markPublishFailure(event, error);
        log.error(
                "{}: outboxId={}, source={}, eventId={}, exchange={}, routingKey={}, error={}",
                message,
                event.getId(),
                event.getSource(),
                event.getEventId(),
                event.getExchange(),
                event.getRoutingKey(),
                error,
                exception
        );

        return failureResult(
                event,
                Outcome.PUBLISH_FAILED,
                false,
                false,
                null,
                null,
                error
        );
    }

    private void markPublishFailure(
            OutboxEvent event,
            String error) {

        int updated = outboxEventMapper.markPublishFailure(
                event.getId(),
                leaseOwner,
                error
        );

        if (updated != 1) {
            log.warn(
                    "Outbox 状态或 Lease Owner 已变化，跳过失败计数更新: outboxId={}, source={}, eventId={}, exchange={}, routingKey={}, leaseOwner={}",
                    event.getId(),
                    event.getSource(),
                    event.getEventId(),
                    event.getExchange(),
                    event.getRoutingKey(),
                    leaseOwner
            );
        }
    }

    private PublishResult failureResult(
            OutboxEvent event,
            Outcome outcome,
            boolean confirmed,
            boolean returned,
            Integer replyCode,
            String replyText,
            String error) {

        return new PublishResult(
                event.getId(),
                outcome,
                confirmed,
                returned,
                replyCode,
                replyText,
                error
        );
    }

    private String exceptionMessage(Exception exception) {

        Throwable current = exception;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        return message == null
                ? current.getClass().getSimpleName()
                : current.getClass().getSimpleName() + ": " + message;
    }

    public enum Outcome {
        SENT,
        RETURNED,
        NACK,
        PUBLISH_FAILED,
        STATE_CHANGED
    }

    public record PublishResult(
            Long outboxId,
            Outcome outcome,
            boolean confirmed,
            boolean returned,
            Integer replyCode,
            String replyText,
            String error) {
    }
}
