package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.entity.SysPermission;
import com.smartfactory.mapper.SysPermissionMapper;
import com.smartfactory.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public List<SysPermission> findAll() {
        return sysPermissionMapper.findAll();
    }

    @Override
    public SysPermission findById(Long id) {

        SysPermission permission = sysPermissionMapper.findById(id);

        if (permission == null) {
            throw new BusinessException(40401, "权限不存在");
        }

        return permission;
    }
}
