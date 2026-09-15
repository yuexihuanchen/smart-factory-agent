package com.smartfactory.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Alarm {

    private Long id;

    private Long deviceId;

    private String alarmCode;

    private String alarmType;

    private String alarmLevel;

    private String title;

    private String message;

    private String status;

    private LocalDateTime firstOccurredAt;

    private LocalDateTime lastOccurredAt;

    private LocalDateTime acknowledgedAt;

    private LocalDateTime resolvedAt;

    private Long acknowledgedBy;

    private Integer occurrenceCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
