package com.smartfactory.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class JwtTokenBlacklistService {

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 将 JWT Token 加入 Redis 黑名单。
     *
     * @param token JWT Token
     * @param ttlSeconds Token 剩余有效时间，单位：秒
     */
    public void blacklist(String token, long ttlSeconds) {

        if (token == null || token.isBlank()) {
            return;
        }

        if (ttlSeconds <= 0) {
            return;
        }

        String key = buildBlacklistKey(token);

        stringRedisTemplate.opsForValue().set(
                key,
                "1",
                ttlSeconds,
                java.util.concurrent.TimeUnit.SECONDS
        );
    }

    /**
     * 判断 JWT Token 是否已经被加入黑名单。
     *
     * @param token JWT Token
     * @return true 表示已经被注销
     */
    public boolean isBlacklisted(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }

        String key = buildBlacklistKey(token);

        Boolean exists = stringRedisTemplate.hasKey(key);

        return Boolean.TRUE.equals(exists);
    }

    /**
     * 根据 JWT Token 生成 Redis 黑名单 Key。
     *
     * 不直接将完整 JWT 存入 Redis Key，
     * 而是使用 SHA-256 生成固定长度的 Token 指纹。
     */
    private String buildBlacklistKey(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(StandardCharsets.UTF_8)
                    );

            String tokenDigest =
                    HexFormat.of().formatHex(hash);

            return KEY_PREFIX + tokenDigest;

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    e
            );
        }
    }
}
