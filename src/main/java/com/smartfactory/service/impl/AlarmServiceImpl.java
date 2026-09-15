package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.common.response.PageResult;
import com.smartfactory.dto.AlarmCreateRequest;
import com.smartfactory.dto.AlarmQueryRequest;
import com.smartfactory.entity.Alarm;
import com.smartfactory.entity.Device;
import com.smartfactory.entity.SysUser;
import com.smartfactory.enums.AlarmLevel;
import com.smartfactory.enums.AlarmStatus;
import com.smartfactory.mapper.AlarmMapper;
import com.smartfactory.mapper.DeviceMapper;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private final AlarmMapper alarmMapper;

    private final DeviceMapper deviceMapper;

    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional
    public Alarm create(AlarmCreateRequest request) {

        validateCreateRequest(request);

        Device device = deviceMapper.findById(request.getDeviceId());

        if (device == null) {
            throw new BusinessException(40401, "设备不存在");
        }

        alarmMapper.upsert(buildNewAlarm(request));

        Alarm alarm = alarmMapper.findOpenByDeviceIdAndAlarmCode(
                request.getDeviceId(),
                request.getAlarmCode()
        );

        if (alarm == null) {
            throw new BusinessException(40905, "告警聚合失败，请重试");
        }

        return alarm;
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

    private void validateCreateRequest(AlarmCreateRequest request) {

        if (request.getAlarmCode() == null
                || request.getAlarmCode().isBlank()) {
            throw new BusinessException(40001, "告警编码不能为空");
        }

        if (!AlarmLevel.isValid(request.getAlarmLevel())) {
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

    private Alarm buildNewAlarm(AlarmCreateRequest request) {

        Alarm alarm = new Alarm();

        alarm.setDeviceId(request.getDeviceId());
        alarm.setAlarmCode(request.getAlarmCode());
        alarm.setAlarmType(request.getAlarmType());
        alarm.setAlarmLevel(request.getAlarmLevel());
        alarm.setTitle(request.getTitle());
        alarm.setMessage(request.getMessage());
        alarm.setStatus(AlarmStatus.ACTIVE.name());
        alarm.setFirstOccurredAt(request.getOccurredAt());
        alarm.setLastOccurredAt(request.getOccurredAt());
        alarm.setOccurrenceCount(1);

        return alarm;
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
