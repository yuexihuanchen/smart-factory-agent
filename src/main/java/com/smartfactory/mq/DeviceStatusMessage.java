package com.smartfactory.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusMessage {

    private Long deviceId;

    private String deviceCode;

    private String status;
}