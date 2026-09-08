package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.entity.Device;
import com.smartfactory.mapper.DeviceMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceServiceImplTest {

    @Test
    void createRefetchesPersistedDevice() {

        DeviceMapper mapper = mock(DeviceMapper.class);
        DeviceServiceImpl service = new DeviceServiceImpl(mapper);

        Device request = new Device();
        request.setDeviceCode("RBAC-TEST-01");
        request.setDeviceName("RBAC Test Device");

        doAnswer(invocation -> {
            Device saved = invocation.getArgument(0);
            saved.setId(6L);
            return 1;
        }).when(mapper).insert(any(Device.class));

        Device persisted = new Device();
        persisted.setId(6L);
        persisted.setDeviceCode("RBAC-TEST-01");
        persisted.setDeviceName("RBAC Test Device");
        persisted.setCreatedAt(LocalDateTime.of(2026, 9, 8, 9, 30));
        persisted.setUpdatedAt(LocalDateTime.of(2026, 9, 8, 9, 30));
        when(mapper.findById(6L)).thenReturn(persisted);

        Device result = service.create(request);

        assertThat(result.getDeviceCode()).isEqualTo("RBAC-TEST-01");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateRefetchesPersistedDevice() {

        DeviceMapper mapper = mock(DeviceMapper.class);
        DeviceServiceImpl service = new DeviceServiceImpl(mapper);

        Device request = new Device();
        request.setId(6L);
        request.setDeviceName("RBAC Test Device Updated");

        when(mapper.update(request)).thenReturn(1);

        Device persisted = new Device();
        persisted.setId(6L);
        persisted.setDeviceCode("RBAC-TEST-01");
        persisted.setDeviceName("RBAC Test Device Updated");
        persisted.setCreatedAt(LocalDateTime.of(2026, 9, 8, 9, 30));
        persisted.setUpdatedAt(LocalDateTime.of(2026, 9, 8, 9, 31));
        when(mapper.findById(6L)).thenReturn(persisted);

        Device result = service.update(request);

        verify(mapper).findById(6L);
        assertThat(result.getDeviceCode()).isEqualTo("RBAC-TEST-01");
        assertThat(result.getUpdatedAt()).isEqualTo(
                LocalDateTime.of(2026, 9, 8, 9, 31)
        );
    }

    @Test
    void updateThrowsNotFoundWhenNoRowChanged() {

        DeviceMapper mapper = mock(DeviceMapper.class);
        DeviceServiceImpl service = new DeviceServiceImpl(mapper);

        Device request = new Device();
        request.setId(999L);
        when(mapper.update(request)).thenReturn(0);

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40401);

        verify(mapper, never()).findById(999L);
    }
}
