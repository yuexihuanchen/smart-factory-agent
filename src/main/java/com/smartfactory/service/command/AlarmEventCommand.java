package com.smartfactory.service.command;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlarmEventCommand {

    private String source;

    private String eventId;

    private Long deviceId;

    private String alarmCode;

    private String alarmType;

    private String alarmLevel;

    private String title;

    private String message;

    private LocalDateTime occurredAt;

    private String payload;
}
