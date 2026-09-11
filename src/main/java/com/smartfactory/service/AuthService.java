package com.smartfactory.service;

import com.smartfactory.dto.LoginRequest;
import com.smartfactory.dto.RefreshTokenRequest;
import com.smartfactory.vo.LoginResponse;
import com.smartfactory.vo.RefreshTokenResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    RefreshTokenResponse refresh(RefreshTokenRequest request);

    void logout(String accessToken, String refreshToken);
}