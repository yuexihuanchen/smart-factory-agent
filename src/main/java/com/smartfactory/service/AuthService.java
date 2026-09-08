package com.smartfactory.service;

import com.smartfactory.dto.LoginRequest;
import com.smartfactory.vo.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
