package com.smartfactory.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAlarmEventMessage {

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
