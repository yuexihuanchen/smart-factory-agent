package com.smartfactory.service.impl;

import com.smartfactory.dto.LoginRequest;
import com.smartfactory.security.JwtTokenBlacklistService;
import com.smartfactory.security.JwtTokenService;
import com.smartfactory.service.AuthService;
import com.smartfactory.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenService jwtTokenService;

    private final JwtTokenBlacklistService jwtTokenBlacklistService;

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

        String token = jwtTokenService.generateToken(userDetails);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUsername(userDetails.getUsername());
        response.setExpiresInSeconds(
                jwtTokenService.getExpirationSeconds()
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
    public void logout(String token) {

        long remainingSeconds =
                jwtTokenService.getRemainingExpirationSeconds(token);

        jwtTokenBlacklistService.blacklist(
                token,
                remainingSeconds
        );
    }
}
