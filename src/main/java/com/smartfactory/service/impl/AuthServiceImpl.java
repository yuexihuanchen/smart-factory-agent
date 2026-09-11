package com.smartfactory.service.impl;

import com.smartfactory.dto.LoginRequest;
import com.smartfactory.dto.RefreshTokenRequest;
import com.smartfactory.security.JwtTokenBlacklistService;
import com.smartfactory.security.JwtTokenService;
import com.smartfactory.security.RefreshTokenService;
import com.smartfactory.service.AuthService;
import com.smartfactory.vo.LoginResponse;
import com.smartfactory.vo.RefreshTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.smartfactory.dto.LogoutRequest;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenService jwtTokenService;

    private final RefreshTokenService refreshTokenService;

    private final JwtTokenBlacklistService jwtTokenBlacklistService;

    @Override
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        String username =
            refreshTokenService.getUsername(refreshToken);

        if (username == null) {
            throw new BadCredentialsException(
                    "刷新令牌无效或已过期"
            );
        }

        UserDetails userDetails;

    try {
        userDetails =
                userDetailsService.loadUserByUsername(username);
    } catch (UsernameNotFoundException e) {
        throw new BadCredentialsException(
                "用户不存在"
        );
    }

    if (!userDetails.isEnabled()) {
        throw new BadCredentialsException(
                "用户已禁用"
        );
    }

    // Refresh Token Rotation：
    // 当前 Refresh Token 使用后立即失效
    refreshTokenService.revoke(refreshToken);

    String newAccessToken =
            jwtTokenService.generateToken(userDetails);

    String newRefreshToken =
            refreshTokenService.create(username);

    RefreshTokenResponse response =
            new RefreshTokenResponse();

    response.setAccessToken(newAccessToken);
    response.setTokenType("Bearer");
    response.setExpiresInSeconds(
            jwtTokenService.getExpirationSeconds()
    );

    response.setRefreshToken(newRefreshToken);
    response.setRefreshExpiresInSeconds(
            refreshTokenService.getExpirationSeconds()
    );

    response.setUsername(username);

    response.setAuthorities(
            userDetails.getAuthorities()
                    .stream()
                    .map(authority -> authority.getAuthority())
                    .toList()
    );

        return response;
    }


    @Override
    public LoginResponse login(LoginRequest request) {

        UserDetails userDetails;

        try {
            userDetails = userDetailsService.loadUserByUsername(
                    request.getUsername()
            );
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                userDetails.getPassword()
        )) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        if (!userDetails.isEnabled()) {
            throw new BadCredentialsException("用户已禁用");
        }

        String accessToken =
                jwtTokenService.generateToken(userDetails);

        String refreshToken =
                refreshTokenService.create(
                        userDetails.getUsername()
                );

        LoginResponse response = new LoginResponse();

        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");

        response.setExpiresInSeconds(
                jwtTokenService.getExpirationSeconds()
        );

        response.setRefreshToken(refreshToken);

        response.setRefreshExpiresInSeconds(
                refreshTokenService.getExpirationSeconds()
        );

        response.setUsername(
                userDetails.getUsername()
        );

        response.setAuthorities(
                userDetails.getAuthorities()
                        .stream()
                        .map(authority -> authority.getAuthority())
                        .toList()
        );

        return response;
    }


  @Override
  public void logout(String accessToken, String refreshToken) {
      long remainingSeconds =
          jwtTokenService.getRemainingExpirationSeconds(accessToken);

      jwtTokenBlacklistService.blacklist(
        accessToken,
        remainingSeconds
      );

      refreshTokenService.revoke(refreshToken);
  }
}