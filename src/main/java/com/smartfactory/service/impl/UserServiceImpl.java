package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.dto.UserCreateRequest;
import com.smartfactory.entity.SysUser;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.service.UserService;
import com.smartfactory.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public UserVO create(UserCreateRequest request) {

        // 1. 检查用户名是否已经存在
        SysUser existingUser =
                sysUserMapper.findByUsername(
                        request.getUsername()
                );

        if (existingUser != null) {
            throw new BusinessException(
                    40001,
                    "用户名已存在"
            );
        }

        // 2. 创建用户对象
        SysUser user = new SysUser();

        user.setUsername(
                request.getUsername()
        );

        user.setNickname(
                request.getNickname()
        );

        user.setStatus("ENABLED");

        // 3. BCrypt 加密密码
        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // 4. 保存到数据库
        sysUserMapper.insert(user);
        
        user = sysUserMapper.findById(user.getId());
        // 5. Entity → VO
        UserVO vo = new UserVO();

        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());

        return vo;
    }
}