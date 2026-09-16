package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.common.response.PageResult;
import com.smartfactory.config.RabbitMQConfig;
import com.smartfactory.dto.AlarmQueryRequest;
import com.smartfactory.entity.Alarm;
import com.smartfactory.entity.AlarmEvent;
import com.smartfactory.entity.Device;
import com.smartfactory.entity.OutboxEvent;
import com.smartfactory.entity.SysUser;
import com.smartfactory.enums.AlarmEventStatus;
import com.smartfactory.enums.AlarmLevel;
import com.smartfactory.enums.AlarmStatus;
import com.smartfactory.mapper.AlarmEventMapper;
import com.smartfactory.mapper.AlarmMapper;
import com.smartfactory.mapper.DeviceMapper;
import com.smartfactory.mapper.OutboxEventMapper;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.mq.DeviceAlarmEventMessage;
import com.smartfactory.service.AlarmService;
import com.smartfactory.service.command.AlarmEventCommand;
import com.smartfactory.vo.AlarmProcessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private final AlarmMapper alarmMapper;

    private final AlarmEventMapper alarmEventMapper;

    private final OutboxEventMapper outboxEventMapper;

    private final DeviceMapper deviceMapper;

    private final SysUserMapper sysUserMapper;

    private final JsonMapper jsonMapper;

    @Override
    @Transactional
    public AlarmProcessResponse processEvent(AlarmEventCommand command) {

        validateEventRequest(command);

        AlarmEvent event = buildAlarmEvent(command);
        int inserted = alarmEventMapper.insert(event);

        if (inserted == 0) {
            return handleDuplicateEvent(command);
        }

        Device device = deviceMapper.findById(command.getDeviceId());

        if (device == null) {
            throw new BusinessException(40401, "设备不存在");
        }

        alarmMapper.upsert(buildNewAlarm(command));

        Alarm alarm = requireOpenAlarm(command);

        int rows = alarmEventMapper.updateAlarmId(
                event.getId(),
                alarm.getId()
        );

        if (rows == 0) {
            throw new BusinessException(40906, "关联告警事件失败");
        }

        outboxEventMapper.insert(buildOutboxEvent(command));

        return new AlarmProcessResponse(
                AlarmEventStatus.CREATED,
                alarm
        );
    }

    private AlarmProcessResponse handleDuplicateEvent(
            AlarmEventCommand command) {

        AlarmEvent event = alarmEventMapper.findBySourceAndEventId(
                command.getSource(),
                command.getEventId()
        );

        if (event == null || event.getAlarmId() == null) {
            throw new BusinessException(
                    40905,
                    "重复事件关联告警不存在"
            );
        }

        Alarm alarm = alarmMapper.findById(event.getAlarmId());

        if (alarm == null) {
            throw new BusinessException(
                    40905,
                    "重复事件关联告警不存在"
            );
        }

        return new AlarmProcessResponse(
                AlarmEventStatus.DUPLICATE,
                alarm
        );
    }

    @Override
    public PageResult<Alarm> findPage(AlarmQueryRequest request) {

        validateQueryRequest(request);

        int page = request.getPage();
        int size = request.getSize();
        int offset = (page - 1) * size;

        List<Alarm> records = alarmMapper.findPage(
                offset,
                size,
                request.getDeviceId(),
                request.getAlarmCode(),
                request.getAlarmLevel(),
                request.getStatus(),
                request.getKeyword(),
                request.getStartTime(),
                request.getEndTime()
        );

        long total = alarmMapper.count(
                request.getDeviceId(),
                request.getAlarmCode(),
                request.getAlarmLevel(),
                request.getStatus(),
                request.getKeyword(),
                request.getStartTime(),
                request.getEndTime()
        );

        return new PageResult<>(
                records,
                page,
                size,
                total
        );
    }

    @Override
    public Alarm findById(Long id) {
        return requireAlarm(id);
    }

    @Override
    @Transactional
    public Alarm acknowledge(Long id, String username) {

        Alarm alarm = requireAlarm(id);
        AlarmStatus status = requireStatus(alarm.getStatus());

        if (status == AlarmStatus.ACKNOWLEDGED) {
            throw new BusinessException(40901, "告警已确认，请勿重复确认");
        }

        if (status == AlarmStatus.RESOLVED) {
            throw new BusinessException(40902, "已恢复的告警不能再次确认");
        }

        if (status != AlarmStatus.ACTIVE) {
            throw new BusinessException(40012, "只有活动告警可以确认");
        }

        SysUser user = sysUserMapper.findByUsername(username);

        if (user == null) {
            throw new BusinessException(40101, "当前用户不存在");
        }

        LocalDateTime now = LocalDateTime.now();

        int rows = alarmMapper.acknowledge(
                id,
                now,
                user.getId()
        );

        if (rows == 0) {
            throw new BusinessException(
                    40904,
                    "告警状态已变化，请刷新后重试"
            );
        }

        return requireAlarm(id);
    }

    @Override
    @Transactional
    public Alarm resolve(Long id) {

        Alarm alarm = requireAlarm(id);
        AlarmStatus status = requireStatus(alarm.getStatus());

        if (status == AlarmStatus.RESOLVED) {
            throw new BusinessException(40903, "告警已经恢复");
        }

        if (status != AlarmStatus.ACTIVE
                && status != AlarmStatus.ACKNOWLEDGED) {
            throw new BusinessException(
                    40011,
                    "告警状态非法"
            );
        }

        int rows = alarmMapper.resolve(id, LocalDateTime.now());

        if (rows == 0) {
            throw new BusinessException(
                    40904,
                    "告警状态已变化，请刷新后重试"
            );
        }

        return requireAlarm(id);
    }

    private void validateEventRequest(AlarmEventCommand command) {

        if (command.getDeviceId() == null
                || command.getDeviceId() <= 0) {
            throw new BusinessException(40001, "设备ID必须大于0");
        }

        if (command.getSource() == null
                || command.getSource().isBlank()
                || command.getSource().length() > 64
                || !isAscii(command.getSource())) {
            throw new BusinessException(40001, "事件来源非法");
        }

        if (command.getEventId() == null
                || command.getEventId().isBlank()
                || command.getEventId().length() > 128
                || !isAscii(command.getEventId())) {
            throw new BusinessException(40001, "事件ID非法");
        }

        if (command.getAlarmCode() == null
                || command.getAlarmCode().isBlank()
                || command.getAlarmCode().length() > 64) {
            throw new BusinessException(40001, "告警编码不能为空");
        }

        if (command.getOccurredAt() == null) {
            throw new BusinessException(40001, "故障发生时间不能为空");
        }

        if (!AlarmLevel.isValid(command.getAlarmLevel())) {
            throw new BusinessException(40010, "告警等级非法");
        }
    }

    private void validateQueryRequest(AlarmQueryRequest request) {

        if (request.getAlarmLevel() != null
                && !request.getAlarmLevel().isBlank()
                && !AlarmLevel.isValid(request.getAlarmLevel())) {
            throw new BusinessException(40010, "告警等级非法");
        }

        if (request.getStatus() != null
                && !request.getStatus().isBlank()
                && !AlarmStatus.isValid(request.getStatus())) {
            throw new BusinessException(40011, "告警状态非法");
        }

        if (request.getStartTime() != null
                && request.getEndTime() != null
                && request.getStartTime().isAfter(request.getEndTime())) {
            throw new BusinessException(
                    40014,
                    "开始时间不能晚于结束时间"
            );
        }
    }

    private Alarm buildNewAlarm(AlarmEventCommand command) {

        Alarm alarm = new Alarm();

        alarm.setDeviceId(command.getDeviceId());
        alarm.setAlarmCode(command.getAlarmCode());
        alarm.setAlarmType(command.getAlarmType());
        alarm.setAlarmLevel(command.getAlarmLevel());
        alarm.setTitle(command.getTitle());
        alarm.setMessage(command.getMessage());
        alarm.setStatus(AlarmStatus.ACTIVE.name());
        alarm.setFirstOccurredAt(command.getOccurredAt());
        alarm.setLastOccurredAt(command.getOccurredAt());
        alarm.setOccurrenceCount(1);

        return alarm;
    }

    private AlarmEvent buildAlarmEvent(AlarmEventCommand command) {

        AlarmEvent event = new AlarmEvent();

        event.setSource(command.getSource());
        event.setEventId(command.getEventId());
        event.setDeviceId(command.getDeviceId());
        event.setAlarmCode(command.getAlarmCode());
        event.setOccurredAt(command.getOccurredAt());
        event.setPayload(command.getPayload());

        return event;
    }

    private OutboxEvent buildOutboxEvent(
            AlarmEventCommand command) {

        DeviceAlarmEventMessage message =
                new DeviceAlarmEventMessage();

        message.setSource(command.getSource());
        message.setEventId(command.getEventId());
        message.setDeviceId(command.getDeviceId());
        message.setAlarmCode(command.getAlarmCode());
        message.setAlarmType(command.getAlarmType());
        message.setAlarmLevel(command.getAlarmLevel());
        message.setTitle(command.getTitle());
        message.setMessage(command.getMessage());
        message.setOccurredAt(command.getOccurredAt());
        message.setPayload(command.getPayload());

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setSource(command.getSource());
        outboxEvent.setEventId(command.getEventId());
        outboxEvent.setExchange(RabbitMQConfig.DEVICE_EXCHANGE);
        outboxEvent.setRoutingKey(
                RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY
        );
        outboxEvent.setPayload(
                jsonMapper.writeValueAsString(message)
        );
        outboxEvent.setStatus("PENDING");
        outboxEvent.setRetryCount(0);
        outboxEvent.setPublishedAt(null);
        outboxEvent.setLastError(null);

        return outboxEvent;
    }

    private Alarm requireOpenAlarm(AlarmEventCommand command) {

        Alarm alarm = alarmMapper.findOpenByDeviceIdAndAlarmCode(
                command.getDeviceId(),
                command.getAlarmCode()
        );

        if (alarm == null) {
            throw new BusinessException(40905, "告警聚合失败，请重试");
        }

        return alarm;
    }

    private boolean isAscii(String value) {
        return value.chars().allMatch(character -> character <= 0x7F);
    }

    private Alarm requireAlarm(Long id) {

        Alarm alarm = alarmMapper.findById(id);

        if (alarm == null) {
            throw new BusinessException(40402, "告警不存在");
        }

        return alarm;
    }

    private AlarmStatus requireStatus(String value) {

        if (!AlarmStatus.isValid(value)) {
            throw new BusinessException(40011, "告警状态非法");
        }

        return AlarmStatus.valueOf(value);
    }
}
