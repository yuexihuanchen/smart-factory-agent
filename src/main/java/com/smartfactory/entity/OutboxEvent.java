package com.smartfactory.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OutboxEvent {

    private Long id;

    private String source;

    private String eventId;

    private String exchange;

    private String routingKey;

    private String payload;

    private String status;

    private Integer retryCount;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    private String lastError;
}
