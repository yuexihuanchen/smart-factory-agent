package com.smartfactory.security;

import com.smartfactory.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class JwtTokenService {

    private final SecretKey key;

    private final long expirationSeconds;

    public JwtTokenService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
        this.expirationSeconds = properties.getExpirationSeconds();
    }

    public String generateToken(UserDetails userDetails) {

        Date now = new Date();
        Date expiresAt = new Date(
                now.getTime() + expirationSeconds * 1000
        );

        List<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .toList();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("authorities", authorities)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, String username) {

        try {
            return username.equals(parseClaims(token).getSubject());
        } catch (RuntimeException e) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
