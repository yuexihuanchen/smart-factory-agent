package com.smartfactory.service;

import com.smartfactory.entity.SysPermission;

import java.util.List;

public interface PermissionService {

    List<SysPermission> findAll();

    SysPermission findById(Long id);
}
