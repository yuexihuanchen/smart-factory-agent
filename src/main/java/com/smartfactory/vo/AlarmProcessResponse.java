package com.smartfactory.vo;

import com.smartfactory.entity.Alarm;
import com.smartfactory.enums.AlarmEventStatus;
import lombok.Data;

@Data
public class AlarmProcessResponse {

    private AlarmEventStatus eventStatus;

    private Alarm alarm;

    public AlarmProcessResponse(
            AlarmEventStatus eventStatus,
            Alarm alarm) {
        this.eventStatus = eventStatus;
        this.alarm = alarm;
    }
}
