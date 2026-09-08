package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.dto.UserCreateRequest;
import com.smartfactory.dto.UserUpdateRequest;
import com.smartfactory.entity.SysPermission;
import com.smartfactory.entity.SysRole;
import com.smartfactory.entity.SysUser;
import com.smartfactory.mapper.SysPermissionMapper;
import com.smartfactory.mapper.SysRoleMapper;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.mapper.SysUserRoleMapper;
import com.smartfactory.service.UserService;
import com.smartfactory.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysPermissionMapper sysPermissionMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserVO> findAll() {

        return sysUserMapper.findAll()
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public UserVO findById(Long id) {

        return toVO(getExistingUser(id));
    }

    @Override
    public UserVO create(UserCreateRequest request) {

        SysUser existingUser = sysUserMapper.findByUsername(
                request.getUsername()
        );

        if (existingUser != null) {
            throw new BusinessException(40001, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setStatus("ENABLED");
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        sysUserMapper.insert(user);

        return toVO(sysUserMapper.findById(user.getId()));
    }

    @Override
    public UserVO update(Long id, UserUpdateRequest request) {

        getExistingUser(id);

        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname(request.getNickname());
        user.setStatus(request.getStatus());

        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {
            user.setPassword(
                    passwordEncoder.encode(request.getPassword())
            );
        }

        sysUserMapper.update(user);

        return toVO(sysUserMapper.findById(id));
    }

    @Override
    public void deleteById(Long id) {

        getExistingUser(id);
        sysUserRoleMapper.deleteByUserId(id);
        sysUserMapper.deleteById(id);
    }

    @Override
    public UserVO assignRoles(Long id, List<Long> roleIds) {

        getExistingUser(id);

        for (Long roleId : roleIds) {
            if (sysRoleMapper.findById(roleId) == null) {
                throw new BusinessException(40001, "角色不存在");
            }
        }

        sysUserRoleMapper.deleteByUserId(id);

        for (Long roleId : roleIds) {
            sysUserRoleMapper.insertIgnore(id, roleId);
        }

        return toVO(sysUserMapper.findById(id));
    }

    private SysUser getExistingUser(Long id) {

        SysUser user = sysUserMapper.findById(id);

        if (user == null) {
            throw new BusinessException(40401, "用户不存在");
        }

        return user;
    }

    private UserVO toVO(SysUser user) {

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());

        List<String> roles = sysRoleMapper.findRolesByUserId(
                        user.getId()
                ).stream()
                .map(SysRole::getRoleCode)
                .toList();

        List<String> permissions = sysPermissionMapper
                .findPermissionsByUserId(user.getId())
                .stream()
                .map(SysPermission::getPermissionCode)
                .toList();

        vo.setRoles(roles);
        vo.setPermissions(permissions);
        return vo;
    }
}
