package com.smartfactory.mq;

import com.smartfactory.config.RabbitMQConfig;
import com.smartfactory.service.AlarmService;
import com.smartfactory.service.command.AlarmEventCommand;
import com.smartfactory.vo.AlarmProcessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceAlarmEventConsumer {

    private final AlarmService alarmService;

    @RabbitListener(queues = RabbitMQConfig.DEVICE_ALARM_QUEUE)
    public void consume(DeviceAlarmEventMessage message) {

        validateMessage(message);

        AlarmProcessResponse response = alarmService.processEvent(
                toCommand(message)
        );

        log.info(
                "设备告警事件处理完成: source={}, eventId={}, eventStatus={}, alarmId={}",
                message.getSource(),
                message.getEventId(),
                response.getEventStatus(),
                response.getAlarm().getId()
        );
    }

    private void validateMessage(DeviceAlarmEventMessage message) {

        if (message == null) {
            throw new IllegalArgumentException("告警消息不能为空");
        }

        if (message.getSource() == null
                || message.getSource().isBlank()) {
            throw new IllegalArgumentException("事件来源不能为空");
        }

        if (message.getEventId() == null
                || message.getEventId().isBlank()) {
            throw new IllegalArgumentException("事件ID不能为空");
        }

        if (message.getDeviceId() == null
                || message.getDeviceId() <= 0) {
            throw new IllegalArgumentException("设备ID必须大于0");
        }

        if (message.getAlarmCode() == null
                || message.getAlarmCode().isBlank()) {
            throw new IllegalArgumentException("告警编码不能为空");
        }

        if (message.getAlarmLevel() == null
                || message.getAlarmLevel().isBlank()) {
            throw new IllegalArgumentException("告警等级不能为空");
        }

        if (message.getOccurredAt() == null) {
            throw new IllegalArgumentException("故障发生时间不能为空");
        }
    }

    private AlarmEventCommand toCommand(
            DeviceAlarmEventMessage message) {

        AlarmEventCommand command = new AlarmEventCommand();

        command.setSource(message.getSource());
        command.setEventId(message.getEventId());
        command.setDeviceId(message.getDeviceId());
        command.setAlarmCode(message.getAlarmCode());
        command.setAlarmType(message.getAlarmType());
        command.setAlarmLevel(message.getAlarmLevel());
        command.setTitle(message.getTitle());
        command.setMessage(message.getMessage());
        command.setOccurredAt(message.getOccurredAt());
        command.setPayload(message.getPayload());

        return command;
    }
}
