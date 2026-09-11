package com.smartfactory.service.impl;

import com.smartfactory.dto.LoginRequest;
import com.smartfactory.security.JwtTokenBlacklistService;
import com.smartfactory.security.JwtTokenService;
import com.smartfactory.security.RefreshTokenService;
import com.smartfactory.vo.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    @Test
    void loginReturnsAccessTokenRefreshTokenAndAuthorities() {

        UserDetailsService userDetailsService =
                mock(UserDetailsService.class);
        PasswordEncoder passwordEncoder =
                mock(PasswordEncoder.class);
        JwtTokenService jwtTokenService =
                mock(JwtTokenService.class);
        RefreshTokenService refreshTokenService =
                mock(RefreshTokenService.class);
        JwtTokenBlacklistService jwtTokenBlacklistService =
                mock(JwtTokenBlacklistService.class);

        AuthServiceImpl service = new AuthServiceImpl(
                userDetailsService,
                passwordEncoder,
                jwtTokenService,
                refreshTokenService,
                jwtTokenBlacklistService
        );

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");

        UserDetails user = new User(
                "admin",
                "hash",
                List.of(new SimpleGrantedAuthority("user:create"))
        );

        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(user);

        when(passwordEncoder.matches("123456", "hash"))
                .thenReturn(true);

        when(jwtTokenService.generateToken(user))
                .thenReturn("jwt-token");

        when(jwtTokenService.getExpirationSeconds())
                .thenReturn(7200L);

        when(refreshTokenService.create("admin"))
                .thenReturn("refresh-token");

        when(refreshTokenService.getExpirationSeconds())
                .thenReturn(604800L);

        LoginResponse response = service.login(request);

        assertThat(response.getAccessToken())
                .isEqualTo("jwt-token");

        assertThat(response.getTokenType())
                .isEqualTo("Bearer");

        assertThat(response.getUsername())
                .isEqualTo("admin");

        assertThat(response.getExpiresInSeconds())
                .isEqualTo(7200L);

        assertThat(response.getRefreshToken())
                .isEqualTo("refresh-token");

        assertThat(response.getRefreshExpiresInSeconds())
                .isEqualTo(604800L);

        assertThat(response.getAuthorities())
                .containsExactly("user:create");

        verify(refreshTokenService)
                .create("admin");
    }

    @Test
    void loginRejectsWrongPassword() {

        UserDetailsService userDetailsService =
                mock(UserDetailsService.class);
        PasswordEncoder passwordEncoder =
                mock(PasswordEncoder.class);
        JwtTokenService jwtTokenService =
                mock(JwtTokenService.class);
        RefreshTokenService refreshTokenService =
                mock(RefreshTokenService.class);
        JwtTokenBlacklistService jwtTokenBlacklistService =
                mock(JwtTokenBlacklistService.class);

        AuthServiceImpl service = new AuthServiceImpl(
                userDetailsService,
                passwordEncoder,
                jwtTokenService,
                refreshTokenService,
                jwtTokenBlacklistService
        );

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        UserDetails user = new User(
                "admin",
                "hash",
                List.of(new SimpleGrantedAuthority("user:create"))
        );

        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(user);

        when(passwordEncoder.matches("wrong", "hash"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtTokenService, never())
                .generateToken(user);

        verify(refreshTokenService, never())
                .create("admin");
    }
}