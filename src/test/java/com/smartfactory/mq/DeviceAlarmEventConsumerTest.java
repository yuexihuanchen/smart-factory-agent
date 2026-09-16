package com.smartfactory.mq;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.entity.Alarm;
import com.smartfactory.enums.AlarmEventStatus;
import com.smartfactory.service.AlarmService;
import com.smartfactory.service.command.AlarmEventCommand;
import com.smartfactory.vo.AlarmProcessResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceAlarmEventConsumerTest {

    @Test
    void mapsMessageAndCallsAlarmService() {

        AlarmService alarmService = mock(AlarmService.class);
        DeviceAlarmEventConsumer consumer =
                new DeviceAlarmEventConsumer(alarmService);
        DeviceAlarmEventMessage message = message();

        when(alarmService.processEvent(any()))
                .thenReturn(response());

        consumer.consume(message);

        ArgumentCaptor<AlarmEventCommand> captor =
                ArgumentCaptor.forClass(AlarmEventCommand.class);
        verify(alarmService).processEvent(captor.capture());

        AlarmEventCommand command = captor.getValue();
        assertThat(command.getSource()).isEqualTo("gateway-A");
        assertThat(command.getEventId()).isEqualTo("EVT-RMQ-001");
        assertThat(command.getDeviceId()).isEqualTo(3L);
        assertThat(command.getAlarmCode()).isEqualTo("TEMP_HIGH");
        assertThat(command.getPayload())
                .isEqualTo("{\"temperature\":95}");
    }

    @Test
    void doesNotSwallowServiceException() {

        AlarmService alarmService = mock(AlarmService.class);
        DeviceAlarmEventConsumer consumer =
                new DeviceAlarmEventConsumer(alarmService);

        when(alarmService.processEvent(any()))
                .thenThrow(new BusinessException(
                        40010,
                        "告警等级非法"
                ));

        assertThatThrownBy(() -> consumer.consume(message()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40010);
    }

    @Test
    void rejectsInvalidMessageBeforeServiceCall() {

        AlarmService alarmService = mock(AlarmService.class);
        DeviceAlarmEventConsumer consumer =
                new DeviceAlarmEventConsumer(alarmService);
        DeviceAlarmEventMessage message = message();
        message.setEventId(" ");

        assertThatThrownBy(() -> consumer.consume(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("事件ID不能为空");
    }

    private DeviceAlarmEventMessage message() {

        DeviceAlarmEventMessage message =
                new DeviceAlarmEventMessage();
        message.setSource("gateway-A");
        message.setEventId("EVT-RMQ-001");
        message.setDeviceId(3L);
        message.setAlarmCode("TEMP_HIGH");
        message.setAlarmType("TEMPERATURE");
        message.setAlarmLevel("CRITICAL");
        message.setTitle("设备温度过高");
        message.setMessage("温度超过阈值");
        message.setOccurredAt(
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        message.setPayload("{\"temperature\":95}");
        return message;
    }

    private AlarmProcessResponse response() {

        Alarm alarm = new Alarm();
        alarm.setId(11L);
        return new AlarmProcessResponse(
                AlarmEventStatus.CREATED,
                alarm
        );
    }
}
