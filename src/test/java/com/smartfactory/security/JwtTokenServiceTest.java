package com.smartfactory.security;

import com.smartfactory.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void generateAndParseTokenRoundTrip() {

        JwtTokenService service = new JwtTokenService(
                new JwtProperties(SECRET, 7200L)
        );

        UserDetails user = new User(
                "admin",
                "password",
                List.of(new SimpleGrantedAuthority("device:read"))
        );

        String token = service.generateToken(user);

        assertThat(service.extractUsername(token))
                .isEqualTo("admin");
        assertThat(service.isValid(token, "admin"))
                .isTrue();
        assertThat(service.isValid(token, "other"))
                .isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {

        JwtTokenService service = new JwtTokenService(
                new JwtProperties(SECRET, 7200L)
        );

        UserDetails user = new User(
                "admin",
                "password",
                List.of(new SimpleGrantedAuthority("device:read"))
        );

        String token = service.generateToken(user);
        char[] tamperedChars = token.toCharArray();
        tamperedChars[10] = tamperedChars[10] == 'A' ? 'B' : 'A';
        String tamperedToken = new String(tamperedChars);

        assertThat(service.isValid(tamperedToken, "admin"))
                .isFalse();
    }
}
