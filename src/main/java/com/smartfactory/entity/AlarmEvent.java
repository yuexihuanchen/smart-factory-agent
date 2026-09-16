package com.smartfactory.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlarmEvent {

    private Long id;

    private String source;

    private String eventId;

    private Long deviceId;

    private String alarmCode;

    private LocalDateTime occurredAt;

    private Long alarmId;

    private String payload;

    private LocalDateTime createdAt;
}
