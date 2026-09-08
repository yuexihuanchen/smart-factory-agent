package com.smartfactory.service;

import com.smartfactory.dto.UserCreateRequest;
import com.smartfactory.dto.UserUpdateRequest;
import com.smartfactory.vo.UserVO;

import java.util.List;

public interface UserService {

    List<UserVO> findAll();

    UserVO findById(Long id);

    UserVO create(UserCreateRequest request);

    UserVO update(Long id, UserUpdateRequest request);

    void deleteById(Long id);

    UserVO assignRoles(Long id, List<Long> roleIds);
}
