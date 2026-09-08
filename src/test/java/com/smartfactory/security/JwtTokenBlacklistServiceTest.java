package com.smartfactory.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtTokenBlacklistServiceTest {

    @Test
    void blacklistStoresTokenDigestWithTtl() {

        StringRedisTemplate redisTemplate =
                mock(StringRedisTemplate.class);

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations =
                mock(ValueOperations.class);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        JwtTokenBlacklistService service =
                new JwtTokenBlacklistService(redisTemplate);

        service.blacklist("jwt-token", 100);

        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.startsWith(
                        "jwt:blacklist:"
                ),
                org.mockito.ArgumentMatchers.eq("1"),
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void isBlacklistedReturnsTrueWhenKeyExists() {

        StringRedisTemplate redisTemplate =
                mock(StringRedisTemplate.class);

        JwtTokenBlacklistService service =
                new JwtTokenBlacklistService(redisTemplate);

        when(redisTemplate.hasKey(
                org.mockito.ArgumentMatchers.startsWith(
                        "jwt:blacklist:"
                )
        )).thenReturn(true);

        assertThat(service.isBlacklisted("jwt-token"))
                .isTrue();
    }

    @Test
    void isBlacklistedReturnsFalseWhenKeyDoesNotExist() {

        StringRedisTemplate redisTemplate =
                mock(StringRedisTemplate.class);

        JwtTokenBlacklistService service =
                new JwtTokenBlacklistService(redisTemplate);

        when(redisTemplate.hasKey(
                org.mockito.ArgumentMatchers.startsWith(
                        "jwt:blacklist:"
                )
        )).thenReturn(false);

        assertThat(service.isBlacklisted("jwt-token"))
                .isFalse();
    }

    @Test
    void blacklistIgnoresBlankToken() {

        StringRedisTemplate redisTemplate =
                mock(StringRedisTemplate.class);

        JwtTokenBlacklistService service =
                new JwtTokenBlacklistService(redisTemplate);

        service.blacklist("", 100);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void blacklistIgnoresNonPositiveTtl() {

        StringRedisTemplate redisTemplate =
                mock(StringRedisTemplate.class);

        JwtTokenBlacklistService service =
                new JwtTokenBlacklistService(redisTemplate);

        service.blacklist("jwt-token", 0);

        verify(redisTemplate, never()).opsForValue();
    }
}