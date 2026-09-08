package com.smartfactory.controller;

import com.smartfactory.common.exception.GlobalExceptionHandler;
import com.smartfactory.config.SecurityConfig;
import com.smartfactory.dto.UserCreateRequest;
import com.smartfactory.service.UserService;
import com.smartfactory.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "smart-factory.jwt.secret=test-secret-0123456789abcdef-0123456789abcdef",
        "smart-factory.jwt.expiration-seconds=7200"
})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Configuration
    @Import({SecurityConfig.class, GlobalExceptionHandler.class, UserController.class})
    static class TestConfiguration {

        @Bean
        MockMvcBuilderCustomizer securityMockMvcBuilderCustomizer() {
            return builder -> builder.apply(
                    SecurityMockMvcConfigurers.springSecurity()
            );
        }
    }

    @Test
    @WithMockUser(authorities = "user:create")
    void createUserAllowedWithUserCreate() throws Exception {

        UserVO vo = new UserVO();
        vo.setId(9L);
        vo.setUsername("bob");
        vo.setNickname("Bob");
        vo.setStatus("ENABLED");
        when(userService.create(any(UserCreateRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bob",
                                  "password": "123456",
                                  "nickname": "Bob"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("bob"));
    }

    @Test
    @WithMockUser(authorities = "device:read")
    void createUserForbiddenWithoutUserCreate() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bob",
                                  "password": "123456",
                                  "nickname": "Bob"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }
}
