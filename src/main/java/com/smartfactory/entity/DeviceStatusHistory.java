package com.smartfactory.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "device_status_history")
public class DeviceStatusHistory {

    @Id
    private String id;

    private Long deviceId;

    private String deviceCode;

    private String oldStatus;

    private String newStatus;

    private LocalDateTime timestamp;
}