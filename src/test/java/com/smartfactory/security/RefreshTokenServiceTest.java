package com.smartfactory.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);

        refreshTokenService =
                new RefreshTokenService(stringRedisTemplate);
    }

    @Test
    void create_shouldGenerateTokenAndStoreUsername() {
        String username = "admin";

        String refreshToken =
                refreshTokenService.create(username);

        assertNotNull(refreshToken);
        assertFalse(refreshToken.isBlank());

        verify(stringRedisTemplate)
                .opsForValue();

        ArgumentCaptor<String> keyCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> valueCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<Duration> durationCaptor =
                ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(
                keyCaptor.capture(),
                valueCaptor.capture(),
                durationCaptor.capture()
        );

        assertTrue(
                keyCaptor.getValue()
                        .startsWith("auth:refresh:")
        );

        assertEquals(
                username,
                valueCaptor.getValue()
        );

        assertEquals(
                Duration.ofDays(7),
                durationCaptor.getValue()
        );
    }

    @Test
    void create_shouldGenerateDifferentTokensEachTime() {
        String token1 =
                refreshTokenService.create("admin");

        String token2 =
                refreshTokenService.create("admin");

        assertNotEquals(token1, token2);
    }

    @Test
    void create_shouldRejectBlankUsername() {
        assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService.create(" ")
        );

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void getUsername_shouldReturnUsernameFromRedis() {
        String refreshToken = "test-refresh-token";

        when(valueOperations.get(anyString()))
                .thenReturn("admin");

        String username =
                refreshTokenService.getUsername(
                        refreshToken
                );

        assertEquals("admin", username);

        verify(valueOperations)
                .get(anyString());
    }

    @Test
    void getUsername_shouldReturnNullWhenTokenIsBlank() {
        String username =
                refreshTokenService.getUsername(" ");

        assertNull(username);

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void revoke_shouldDeleteRefreshToken() {
        String refreshToken = "test-refresh-token";

        refreshTokenService.revoke(refreshToken);

        verify(stringRedisTemplate)
                .delete(anyString());
    }

    @Test
    void revoke_shouldIgnoreBlankToken() {
        refreshTokenService.revoke(" ");

        verifyNoInteractions(stringRedisTemplate);
    }
}