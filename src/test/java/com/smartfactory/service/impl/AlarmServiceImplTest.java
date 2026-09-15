package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.common.response.PageResult;
import com.smartfactory.dto.AlarmCreateRequest;
import com.smartfactory.dto.AlarmQueryRequest;
import com.smartfactory.entity.Alarm;
import com.smartfactory.entity.Device;
import com.smartfactory.entity.SysUser;
import com.smartfactory.mapper.AlarmMapper;
import com.smartfactory.mapper.DeviceMapper;
import com.smartfactory.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlarmServiceImplTest {

    private AlarmMapper alarmMapper;

    private DeviceMapper deviceMapper;

    private SysUserMapper sysUserMapper;

    private AlarmServiceImpl service;

    @BeforeEach
    void setUp() {
        alarmMapper = mock(AlarmMapper.class);
        deviceMapper = mock(DeviceMapper.class);
        sysUserMapper = mock(SysUserMapper.class);
        service = new AlarmServiceImpl(
                alarmMapper,
                deviceMapper,
                sysUserMapper
        );
    }

    @Test
    void createCreatesActiveAlarmWhenNoOpenAlarmExists() {

        AlarmCreateRequest request = createRequest();
        request.setOccurredAt(
                LocalDateTime.of(2026, 9, 15, 10, 0)
        );

        when(deviceMapper.findById(3L)).thenReturn(device(3L));
        when(alarmMapper.upsert(any(Alarm.class))).thenReturn(1);

        Alarm persisted = alarm(
                11L,
                "ACTIVE",
                1
        );
        when(alarmMapper.findOpenByDeviceIdAndAlarmCode(
                3L,
                "TEMP_HIGH"
        )).thenReturn(persisted);

        Alarm result = service.create(request);

        assertThat(result.getId()).isEqualTo(11L);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getOccurrenceCount()).isEqualTo(1);
        verify(alarmMapper).upsert(any(Alarm.class));
    }

    @Test
    void createAggregatesRepeatedOpenAlarm() {

        AlarmCreateRequest request = createRequest();
        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 9, 15, 10, 5);
        request.setOccurredAt(occurredAt);

        when(deviceMapper.findById(3L)).thenReturn(device(3L));
        when(alarmMapper.upsert(any(Alarm.class))).thenReturn(2);

        Alarm aggregated = alarm(11L, "ACTIVE", 2);
        aggregated.setLastOccurredAt(occurredAt);
        when(alarmMapper.findOpenByDeviceIdAndAlarmCode(
                3L,
                "TEMP_HIGH"
        )).thenReturn(aggregated);

        Alarm result = service.create(request);

        assertThat(result.getOccurrenceCount()).isEqualTo(2);
        assertThat(result.getLastOccurredAt()).isEqualTo(occurredAt);

        ArgumentCaptor<Alarm> captor =
                ArgumentCaptor.forClass(Alarm.class);
        verify(alarmMapper).upsert(captor.capture());
        assertThat(captor.getValue().getFirstOccurredAt())
                .isEqualTo(occurredAt);
        assertThat(captor.getValue().getLastOccurredAt())
                .isEqualTo(occurredAt);
    }

    @Test
    void createRejectsMissingDevice() {

        when(deviceMapper.findById(3L)).thenReturn(null);

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40401);
    }

    @Test
    void createRejectsBlankAlarmCode() {

        AlarmCreateRequest request = createRequest();
        request.setAlarmCode(" ");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40001);
    }

    @Test
    void createRejectsInvalidAlarmLevel() {

        AlarmCreateRequest request = createRequest();
        request.setAlarmLevel("UNKNOWN");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40010);
    }

    @Test
    void findPagePassesFiltersAndReturnsPageResult() {

        AlarmQueryRequest request = new AlarmQueryRequest();
        request.setPage(2);
        request.setSize(10);
        request.setDeviceId(3L);
        request.setAlarmCode("TEMP_HIGH");
        request.setAlarmLevel("CRITICAL");
        request.setStatus("ACTIVE");
        request.setKeyword("温度");

        LocalDateTime startTime =
                LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime endTime =
                LocalDateTime.of(2026, 9, 30, 23, 59);
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        when(alarmMapper.findPage(
                10,
                10,
                3L,
                "TEMP_HIGH",
                "CRITICAL",
                "ACTIVE",
                "温度",
                startTime,
                endTime
        )).thenReturn(List.of(alarm(11L, "ACTIVE", 2)));

        when(alarmMapper.count(
                3L,
                "TEMP_HIGH",
                "CRITICAL",
                "ACTIVE",
                "温度",
                startTime,
                endTime
        )).thenReturn(1L);

        PageResult<Alarm> result = service.findPage(request);

        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getPages()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    void findPageRejectsInvalidStatus() {

        AlarmQueryRequest request = new AlarmQueryRequest();
        request.setStatus("CLOSED");

        assertThatThrownBy(() -> service.findPage(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40011);
    }

    @Test
    void findByIdReturnsAlarm() {

        Alarm alarm = alarm(11L, "ACTIVE", 1);
        when(alarmMapper.findById(11L)).thenReturn(alarm);

        assertThat(service.findById(11L)).isSameAs(alarm);
    }

    @Test
    void findByIdRejectsMissingAlarm() {

        when(alarmMapper.findById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40402);
    }

    @Test
    void acknowledgeMovesActiveAlarmToAcknowledged() {

        Alarm active = alarm(11L, "ACTIVE", 1);
        Alarm acknowledged = alarm(11L, "ACKNOWLEDGED", 1);
        SysUser user = new SysUser();
        user.setId(7L);

        when(alarmMapper.findById(11L))
                .thenReturn(active, acknowledged);
        when(sysUserMapper.findByUsername("operator"))
                .thenReturn(user);
        when(alarmMapper.acknowledge(
                eq(11L),
                any(LocalDateTime.class),
                eq(7L)
        )).thenReturn(1);

        Alarm result = service.acknowledge(11L, "operator");

        assertThat(result.getStatus()).isEqualTo("ACKNOWLEDGED");
    }

    @Test
    void acknowledgeRejectsDuplicateAcknowledgement() {

        when(alarmMapper.findById(11L))
                .thenReturn(alarm(11L, "ACKNOWLEDGED", 1));

        assertThatThrownBy(() -> service.acknowledge(11L, "operator"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40901);

        verify(alarmMapper, never()).acknowledge(
                any(),
                any(),
                any()
        );
    }

    @Test
    void acknowledgeRejectsResolvedAlarm() {

        when(alarmMapper.findById(11L))
                .thenReturn(alarm(11L, "RESOLVED", 2));

        assertThatThrownBy(() -> service.acknowledge(11L, "operator"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40902);
    }

    @Test
    void resolveMovesActiveAlarmToResolved() {

        Alarm active = alarm(11L, "ACTIVE", 1);
        Alarm resolved = alarm(11L, "RESOLVED", 1);

        when(alarmMapper.findById(11L))
                .thenReturn(active, resolved);
        when(alarmMapper.resolve(
                eq(11L),
                any(LocalDateTime.class)
        )).thenReturn(1);

        Alarm result = service.resolve(11L);

        assertThat(result.getStatus()).isEqualTo("RESOLVED");
    }

    @Test
    void resolveMovesAcknowledgedAlarmToResolved() {

        Alarm acknowledged = alarm(11L, "ACKNOWLEDGED", 2);
        Alarm resolved = alarm(11L, "RESOLVED", 2);

        when(alarmMapper.findById(11L))
                .thenReturn(acknowledged, resolved);
        when(alarmMapper.resolve(
                eq(11L),
                any(LocalDateTime.class)
        )).thenReturn(1);

        Alarm result = service.resolve(11L);

        assertThat(result.getStatus()).isEqualTo("RESOLVED");
    }

    @Test
    void resolveRejectsResolvedAlarm() {

        when(alarmMapper.findById(11L))
                .thenReturn(alarm(11L, "RESOLVED", 2));

        assertThatThrownBy(() -> service.resolve(11L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40903);

        verify(alarmMapper, never()).resolve(any(), any());
    }

    private AlarmCreateRequest createRequest() {

        AlarmCreateRequest request = new AlarmCreateRequest();
        request.setDeviceId(3L);
        request.setAlarmCode("TEMP_HIGH");
        request.setAlarmType("TEMPERATURE");
        request.setAlarmLevel("CRITICAL");
        request.setTitle("设备温度过高");
        request.setMessage("设备3温度持续超过安全阈值");
        return request;
    }

    private Device device(Long id) {

        Device device = new Device();
        device.setId(id);
        return device;
    }

    private Alarm alarm(
            Long id,
            String status,
            int occurrenceCount) {

        LocalDateTime firstOccurredAt =
                LocalDateTime.of(2026, 9, 15, 10, 0);

        Alarm alarm = new Alarm();
        alarm.setId(id);
        alarm.setDeviceId(3L);
        alarm.setAlarmCode("TEMP_HIGH");
        alarm.setAlarmType("TEMPERATURE");
        alarm.setAlarmLevel("CRITICAL");
        alarm.setTitle("设备温度过高");
        alarm.setMessage("设备3温度持续超过安全阈值");
        alarm.setStatus(status);
        alarm.setFirstOccurredAt(firstOccurredAt);
        alarm.setLastOccurredAt(
                firstOccurredAt.plusMinutes(occurrenceCount - 1L)
        );
        alarm.setOccurrenceCount(occurrenceCount);
        alarm.setCreatedAt(firstOccurredAt);
        alarm.setUpdatedAt(firstOccurredAt);
        return alarm;
    }
}
