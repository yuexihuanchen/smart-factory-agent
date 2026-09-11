package com.smartfactory.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "auth:refresh:";

    private static final long REFRESH_TOKEN_TTL_SECONDS =
            Duration.ofDays(7).toSeconds();

    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate stringRedisTemplate;

    private final SecureRandom secureRandom = new SecureRandom();

    public String create(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "username must not be blank"
            );
        }

        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        String refreshToken =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(randomBytes);

        String key = buildKey(refreshToken);

        stringRedisTemplate.opsForValue().set(
                key,
                username,
                Duration.ofSeconds(
                        REFRESH_TOKEN_TTL_SECONDS
                )
        );

        return refreshToken;
    }

    public String getUsername(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }

        return stringRedisTemplate.opsForValue()
                .get(buildKey(refreshToken));
    }

    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        stringRedisTemplate.delete(
                buildKey(refreshToken)
        );
    }

    public long getExpirationSeconds() {
        return REFRESH_TOKEN_TTL_SECONDS;
    }

    private String buildKey(String refreshToken) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            refreshToken.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            String tokenHash =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(hash);

            return KEY_PREFIX + tokenHash;

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    e
            );
        }
    }
}