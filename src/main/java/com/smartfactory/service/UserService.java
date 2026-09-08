package com.smartfactory.service;

import com.smartfactory.dto.UserCreateRequest;
import com.smartfactory.vo.UserVO;

public interface UserService {

    UserVO create(UserCreateRequest request);
}