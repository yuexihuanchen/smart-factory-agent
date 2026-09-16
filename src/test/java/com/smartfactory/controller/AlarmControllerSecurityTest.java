package com.smartfactory.controller;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.common.exception.GlobalExceptionHandler;
import com.smartfactory.common.response.PageResult;
import com.smartfactory.config.SecurityConfig;
import com.smartfactory.entity.Alarm;
import com.smartfactory.enums.AlarmEventStatus;
import com.smartfactory.security.JwtTokenBlacklistService;
import com.smartfactory.service.AlarmService;
import com.smartfactory.vo.AlarmProcessResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlarmController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "smart-factory.jwt.secret=test-secret-0123456789abcdef-0123456789abcdef",
        "smart-factory.jwt.expiration-seconds=7200"
})
class AlarmControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlarmService alarmService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtTokenBlacklistService jwtTokenBlacklistService;

    @Configuration
    @Import({
            SecurityConfig.class,
            GlobalExceptionHandler.class,
            AlarmController.class
    })
    static class TestConfiguration {

        @Bean
        MockMvcBuilderCustomizer securityMockMvcBuilderCustomizer() {
            return builder -> builder.apply(
                    SecurityMockMvcConfigurers.springSecurity()
            );
        }
    }

    @Test
    @WithMockUser(authorities = "alarm:create")
    void createAlarmAllowedWithAlarmCreate() throws Exception {

        AlarmProcessResponse response = new AlarmProcessResponse(
                AlarmEventStatus.CREATED,
                alarm()
        );
        when(alarmService.processEvent(any())).thenReturn(response);

        mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "EDGE-GATEWAY-01",
                                  "eventId": "EVT-20260915-0001",
                                  "deviceId": 3,
                                  "alarmCode": "TEMP_HIGH",
                                  "alarmType": "TEMPERATURE",
                                  "alarmLevel": "CRITICAL",
                                  "title": "设备温度过高",
                                  "message": "温度超过安全阈值",
                                  "occurredAt": "2026-09-15T15:30:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.eventStatus")
                        .value("CREATED"))
                .andExpect(jsonPath("$.data.alarm.id").value(11));
    }

    @Test
    @WithMockUser(authorities = "alarm:create")
    void duplicateAlarmEventReturnsDuplicateStatus() throws Exception {

        AlarmProcessResponse response = new AlarmProcessResponse(
                AlarmEventStatus.DUPLICATE,
                alarm()
        );
        when(alarmService.processEvent(any())).thenReturn(response);

        mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "EDGE-GATEWAY-01",
                                  "eventId": "EVT-20260915-0001",
                                  "deviceId": 3,
                                  "alarmCode": "TEMP_HIGH",
                                  "alarmLevel": "CRITICAL",
                                  "occurredAt": "2026-09-15T15:30:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.eventStatus")
                        .value("DUPLICATE"))
                .andExpect(jsonPath("$.data.alarm.id").value(11));
    }

    @Test
    @WithMockUser(authorities = "device:read")
    void createAlarmForbiddenWithoutAlarmCreate() throws Exception {

        mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "EDGE-GATEWAY-01",
                                  "eventId": "EVT-20260915-0002",
                                  "deviceId": 3,
                                  "alarmCode": "TEMP_HIGH",
                                  "alarmLevel": "CRITICAL",
                                  "occurredAt": "2026-09-15T15:30:00"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    @WithMockUser(authorities = "alarm:create")
    void createAlarmReturnsBadRequestForInvalidParameter() throws Exception {

        mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "EDGE-GATEWAY-01",
                                  "eventId": "EVT-20260915-0003",
                                  "deviceId": 3,
                                  "alarmCode": "TEMP_HIGH",
                                  "occurredAt": "2026-09-15T15:30:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    @WithMockUser(authorities = "alarm:create")
    void createAlarmReturnsNotFoundWhenDeviceDoesNotExist() throws Exception {

        when(alarmService.processEvent(any()))
                .thenThrow(new BusinessException(40401, "设备不存在"));

        mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "EDGE-GATEWAY-01",
                                  "eventId": "EVT-20260915-0004",
                                  "deviceId": 999,
                                  "alarmCode": "TEMP_HIGH",
                                  "alarmLevel": "CRITICAL",
                                  "occurredAt": "2026-09-15T15:30:00"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("设备不存在"));
    }

    @Test
    @WithMockUser(authorities = "alarm:read")
    void queryAlarmsAllowedWithAlarmRead() throws Exception {

        PageResult<Alarm> pageResult = new PageResult<>(
                List.of(alarm()),
                1,
                20,
                1L
        );
        when(alarmService.findPage(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/alarms")
                        .param("page", "1")
                        .param("size", "20")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @WithMockUser(authorities = "device:read")
    void queryAlarmsForbiddenWithoutAlarmRead() throws Exception {

        mockMvc.perform(get("/api/alarms"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    @WithMockUser(authorities = "alarm:ack")
    void acknowledgeAlarmAllowedWithAlarmAck() throws Exception {

        Alarm acknowledged = alarm();
        acknowledged.setStatus("ACKNOWLEDGED");
        when(alarmService.acknowledge(11L, "user"))
                .thenReturn(acknowledged);

        mockMvc.perform(post("/api/alarms/11/ack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status")
                        .value("ACKNOWLEDGED"));
    }

    @Test
    @WithMockUser(authorities = "alarm:read")
    void acknowledgeAlarmForbiddenWithoutAlarmAck() throws Exception {

        mockMvc.perform(post("/api/alarms/11/ack"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    @WithMockUser(authorities = "alarm:ack")
    void acknowledgeAlarmReturnsConflictForIllegalState() throws Exception {

        when(alarmService.acknowledge(11L, "user"))
                .thenThrow(new BusinessException(
                        40901,
                        "告警已确认，请勿重复确认"
                ));

        mockMvc.perform(post("/api/alarms/11/ack"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));
    }

    @Test
    @WithMockUser(authorities = "alarm:resolve")
    void resolveAlarmAllowedWithAlarmResolve() throws Exception {

        Alarm resolved = alarm();
        resolved.setStatus("RESOLVED");
        when(alarmService.resolve(11L)).thenReturn(resolved);

        mockMvc.perform(post("/api/alarms/11/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status")
                        .value("RESOLVED"));
    }

    @Test
    @WithMockUser(authorities = "alarm:read")
    void resolveAlarmForbiddenWithoutAlarmResolve() throws Exception {

        mockMvc.perform(post("/api/alarms/11/resolve"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    @WithMockUser(authorities = "alarm:read")
    void queryAlarmsReturnsInternalServerErrorForUnknownException()
            throws Exception {

        when(alarmService.findPage(any()))
                .thenThrow(new RuntimeException("unexpected"));

        mockMvc.perform(get("/api/alarms"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50000));
    }

    private Alarm alarm() {

        Alarm alarm = new Alarm();
        alarm.setId(11L);
        alarm.setDeviceId(3L);
        alarm.setAlarmCode("TEMP_HIGH");
        alarm.setAlarmType("TEMPERATURE");
        alarm.setAlarmLevel("CRITICAL");
        alarm.setTitle("设备温度过高");
        alarm.setMessage("温度超过安全阈值");
        alarm.setStatus("ACTIVE");
        alarm.setOccurrenceCount(1);
        return alarm;
    }
}
