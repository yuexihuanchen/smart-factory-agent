package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.common.response.PageResult;
import com.smartfactory.dto.AlarmQueryRequest;
import com.smartfactory.entity.Alarm;
import com.smartfactory.entity.AlarmEvent;
import com.smartfactory.entity.Device;
import com.smartfactory.entity.SysUser;
import com.smartfactory.enums.AlarmEventStatus;
import com.smartfactory.mapper.AlarmEventMapper;
import com.smartfactory.mapper.AlarmMapper;
import com.smartfactory.mapper.DeviceMapper;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.service.command.AlarmEventCommand;
import com.smartfactory.vo.AlarmProcessResponse;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlarmServiceImplTest {

    private AlarmMapper alarmMapper;

    private AlarmEventMapper alarmEventMapper;

    private DeviceMapper deviceMapper;

    private SysUserMapper sysUserMapper;

    private AlarmServiceImpl service;

    @BeforeEach
    void setUp() {
        alarmMapper = mock(AlarmMapper.class);
        alarmEventMapper = mock(AlarmEventMapper.class);
        deviceMapper = mock(DeviceMapper.class);
        sysUserMapper = mock(SysUserMapper.class);
        service = new AlarmServiceImpl(
                alarmMapper,
                alarmEventMapper,
                deviceMapper,
                sysUserMapper
        );
    }

    @Test
    void processEventCreatesNewEventAndAlarm() {

        AlarmEventCommand request = createRequest();
        Alarm persisted = alarm(11L, "ACTIVE", 1);

        when(alarmEventMapper.insert(any(AlarmEvent.class)))
                .thenAnswer(invocation -> {
                    AlarmEvent event = invocation.getArgument(0);
                    event.setId(21L);
                    return 1;
                });
        when(deviceMapper.findById(3L)).thenReturn(device(3L));
        when(alarmMapper.upsert(any(Alarm.class))).thenReturn(1);
        when(alarmMapper.findOpenByDeviceIdAndAlarmCode(
                3L,
                "TEMP_HIGH"
        )).thenReturn(persisted);
        when(alarmEventMapper.updateAlarmId(21L, 11L))
                .thenReturn(1);

        AlarmProcessResponse response = service.processEvent(request);

        assertThat(response.getEventStatus())
                .isEqualTo(AlarmEventStatus.CREATED);
        assertThat(response.getAlarm().getId()).isEqualTo(11L);
        assertThat(response.getAlarm().getOccurrenceCount())
                .isEqualTo(1);

        ArgumentCaptor<AlarmEvent> eventCaptor =
                ArgumentCaptor.forClass(AlarmEvent.class);
        verify(alarmEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPayload())
                .isEqualTo("{\"temperature\":95}");
        verify(alarmMapper).upsert(any(Alarm.class));
        verify(alarmEventMapper).updateAlarmId(21L, 11L);
    }

    @Test
    void processDuplicateEventDoesNotUpsertAlarm() {

        AlarmEventCommand request = createRequest();
        AlarmEvent existingEvent = event(21L, 11L);
        Alarm persisted = alarm(11L, "ACTIVE", 1);

        when(alarmEventMapper.insert(any(AlarmEvent.class)))
                .thenReturn(0);
        when(alarmEventMapper.findBySourceAndEventId(
                "EDGE-GATEWAY-01",
                "EVT-20260915-0001"
        )).thenReturn(existingEvent);
        when(alarmMapper.findById(11L)).thenReturn(persisted);

        AlarmProcessResponse response = service.processEvent(request);

        assertThat(response.getEventStatus())
                .isEqualTo(AlarmEventStatus.DUPLICATE);
        assertThat(response.getAlarm().getId()).isEqualTo(11L);

        verify(alarmMapper, never()).upsert(any(Alarm.class));
        verify(deviceMapper, never()).findById(any());
        verify(alarmEventMapper, never())
                .updateAlarmId(any(), any());
    }

    @Test
    void processDifferentEventsAggregatesIntoSameAlarm() {

        AlarmEventCommand firstRequest = createRequest();
        AlarmEventCommand secondRequest = createRequest();
        secondRequest.setEventId("EVT-20260915-0002");
        secondRequest.setOccurredAt(
                LocalDateTime.of(2026, 9, 15, 10, 5)
        );

        when(alarmEventMapper.insert(any(AlarmEvent.class)))
                .thenAnswer(invocation -> {
                    AlarmEvent event = invocation.getArgument(0);
                    event.setId(
                            "EVT-20260915-0001".equals(event.getEventId())
                                    ? 21L
                                    : 22L
                    );
                    return 1;
                });
        when(deviceMapper.findById(3L)).thenReturn(device(3L));
        when(alarmMapper.upsert(any(Alarm.class))).thenReturn(1, 2);

        Alarm firstAlarm = alarm(11L, "ACTIVE", 1);
        Alarm secondAlarm = alarm(11L, "ACTIVE", 2);
        when(alarmMapper.findOpenByDeviceIdAndAlarmCode(
                3L,
                "TEMP_HIGH"
        )).thenReturn(firstAlarm, secondAlarm);
        when(alarmEventMapper.updateAlarmId(any(), eq(11L)))
                .thenReturn(1);

        AlarmProcessResponse first =
                service.processEvent(firstRequest);
        AlarmProcessResponse second =
                service.processEvent(secondRequest);

        assertThat(first.getAlarm().getOccurrenceCount())
                .isEqualTo(1);
        assertThat(second.getAlarm().getOccurrenceCount())
                .isEqualTo(2);
        assertThat(second.getAlarm().getId()).isEqualTo(11L);

        verify(alarmMapper, times(2)).upsert(any(Alarm.class));
        verify(alarmEventMapper).updateAlarmId(21L, 11L);
        verify(alarmEventMapper).updateAlarmId(22L, 11L);
    }

    @Test
    void processNewEventAfterResolvedCreatesNewAlarm() {

        AlarmEventCommand request = createRequest();
        Alarm resolved = alarm(11L, "RESOLVED", 2);
        Alarm newAlarm = alarm(12L, "ACTIVE", 1);

        when(alarmEventMapper.insert(any(AlarmEvent.class)))
                .thenAnswer(invocation -> {
                    AlarmEvent event = invocation.getArgument(0);
                    event.setId(23L);
                    return 1;
                });
        when(deviceMapper.findById(3L)).thenReturn(device(3L));
        when(alarmMapper.upsert(any(Alarm.class))).thenReturn(1);
        when(alarmMapper.findOpenByDeviceIdAndAlarmCode(
                3L,
                "TEMP_HIGH"
        )).thenReturn(newAlarm);
        when(alarmEventMapper.updateAlarmId(23L, 12L))
                .thenReturn(1);

        AlarmProcessResponse response = service.processEvent(request);

        assertThat(response.getEventStatus())
                .isEqualTo(AlarmEventStatus.CREATED);
        assertThat(response.getAlarm().getId()).isEqualTo(12L);
        assertThat(response.getAlarm().getStatus()).isEqualTo("ACTIVE");

        verify(alarmMapper).upsert(any(Alarm.class));
        verify(alarmEventMapper).updateAlarmId(23L, 12L);
        assertThat(resolved.getId()).isEqualTo(11L);
    }

    @Test
    void processEventPassesOccurredAtToAlarmUpsert() {

        AlarmEventCommand request = createRequest();
        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 9, 15, 9, 30);
        request.setOccurredAt(occurredAt);

        when(alarmEventMapper.insert(any(AlarmEvent.class)))
                .thenAnswer(invocation -> {
                    AlarmEvent event = invocation.getArgument(0);
                    event.setId(21L);
                    return 1;
                });
        when(deviceMapper.findById(3L)).thenReturn(device(3L));
        when(alarmMapper.upsert(any(Alarm.class))).thenReturn(1);
        when(alarmMapper.findOpenByDeviceIdAndAlarmCode(
                3L,
                "TEMP_HIGH"
        )).thenReturn(alarm(11L, "ACTIVE", 2));
        when(alarmEventMapper.updateAlarmId(21L, 11L))
                .thenReturn(1);

        service.processEvent(request);

        ArgumentCaptor<Alarm> captor =
                ArgumentCaptor.forClass(Alarm.class);
        verify(alarmMapper).upsert(captor.capture());
        assertThat(captor.getValue().getFirstOccurredAt())
                .isEqualTo(occurredAt);
        assertThat(captor.getValue().getLastOccurredAt())
                .isEqualTo(occurredAt);
    }

    @Test
    void processEventRejectsMissingDeviceAndStopsAlarmAggregation() {

        AlarmEventCommand request = createRequest();

        when(alarmEventMapper.insert(any(AlarmEvent.class)))
                .thenReturn(1);
        when(deviceMapper.findById(3L)).thenReturn(null);

        assertThatThrownBy(() -> service.processEvent(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40401);

        verify(alarmMapper, never()).upsert(any(Alarm.class));
        verify(alarmEventMapper, never())
                .updateAlarmId(any(), any());
    }

    @Test
    void processEventRejectsInvalidEventFields() {

        AlarmEventCommand missingSource = createRequest();
        missingSource.setSource(" ");

        AlarmEventCommand missingEventId = createRequest();
        missingEventId.setEventId(" ");

        AlarmEventCommand missingOccurredAt = createRequest();
        missingOccurredAt.setOccurredAt(null);

        AlarmEventCommand nonAsciiEventId = createRequest();
        nonAsciiEventId.setEventId("事件-001");

        assertThatThrownBy(() -> service.processEvent(missingSource))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40001);

        assertThatThrownBy(() -> service.processEvent(missingEventId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40001);

        assertThatThrownBy(() -> service.processEvent(missingOccurredAt))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40001);

        assertThatThrownBy(() -> service.processEvent(nonAsciiEventId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40001);
    }

    @Test
    void processEventFailsWhenEventCannotLinkAlarm() {

        AlarmEventCommand request = createRequest();

        when(alarmEventMapper.insert(any(AlarmEvent.class)))
                .thenAnswer(invocation -> {
                    AlarmEvent event = invocation.getArgument(0);
                    event.setId(21L);
                    return 1;
                });
        when(deviceMapper.findById(3L)).thenReturn(device(3L));
        when(alarmMapper.upsert(any(Alarm.class))).thenReturn(1);
        when(alarmMapper.findOpenByDeviceIdAndAlarmCode(
                3L,
                "TEMP_HIGH"
        )).thenReturn(alarm(11L, "ACTIVE", 1));
        when(alarmEventMapper.updateAlarmId(21L, 11L))
                .thenReturn(0);

        assertThatThrownBy(() -> service.processEvent(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40906);
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

    private AlarmEventCommand createRequest() {

        AlarmEventCommand request = new AlarmEventCommand();
        request.setSource("EDGE-GATEWAY-01");
        request.setEventId("EVT-20260915-0001");
        request.setDeviceId(3L);
        request.setAlarmCode("TEMP_HIGH");
        request.setAlarmType("TEMPERATURE");
        request.setAlarmLevel("CRITICAL");
        request.setTitle("设备温度过高");
        request.setMessage("设备3温度持续超过安全阈值");
        request.setOccurredAt(
                LocalDateTime.of(2026, 9, 15, 10, 0)
        );
        request.setPayload("{\"temperature\":95}");
        return request;
    }

    private Device device(Long id) {

        Device device = new Device();
        device.setId(id);
        return device;
    }

    private AlarmEvent event(Long id, Long alarmId) {

        AlarmEvent event = new AlarmEvent();
        event.setId(id);
        event.setSource("EDGE-GATEWAY-01");
        event.setEventId("EVT-20260915-0001");
        event.setDeviceId(3L);
        event.setAlarmCode("TEMP_HIGH");
        event.setOccurredAt(
                LocalDateTime.of(2026, 9, 15, 10, 0)
        );
        event.setAlarmId(alarmId);
        return event;
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
