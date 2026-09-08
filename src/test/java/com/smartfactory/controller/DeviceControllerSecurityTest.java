package com.smartfactory.controller;

import com.smartfactory.common.exception.GlobalExceptionHandler;
import com.smartfactory.config.SecurityConfig;
import com.smartfactory.service.DeviceService;
import com.smartfactory.service.DeviceStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeviceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @MockitoBean
    private DeviceStatusService deviceStatusService;

    @Configuration
    @Import({SecurityConfig.class, GlobalExceptionHandler.class, DeviceController.class})
    static class TestConfiguration {

        @Bean
        MockMvcBuilderCustomizer securityMockMvcBuilderCustomizer() {
            return builder -> builder.apply(
                    SecurityMockMvcConfigurers.springSecurity()
            );
        }
    }

    @Test
    @WithMockUser(authorities = "device:read")
    void listDevicesAllowedWithDeviceRead() throws Exception {

        when(deviceService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = "alarm:read")
    void listDevicesForbiddenWithoutDeviceRead() throws Exception {

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    @WithMockUser(authorities = "device:create")
    void createDeviceAllowedWithDeviceCreate() throws Exception {

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceCode": "DEV-TEST-01",
                                  "deviceName": "Test Device",
                                  "deviceType": "CNC",
                                  "location": "Zone A",
                                  "ipAddress": "192.168.1.10",
                                  "port": 502,
                                  "protocol": "MODBUS_TCP",
                                  "status": "RUNNING",
                                  "description": "security test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = "device:read")
    void createDeviceForbiddenWithoutDeviceCreate() throws Exception {

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceCode": "DEV-TEST-02",
                                  "deviceName": "Test Device",
                                  "deviceType": "CNC",
                                  "location": "Zone A",
                                  "ipAddress": "192.168.1.11",
                                  "port": 502,
                                  "protocol": "MODBUS_TCP",
                                  "status": "RUNNING",
                                  "description": "security test"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }
}
