package com.smartfactory.mapper;

import com.smartfactory.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysPermissionMapper {

    List<SysPermission> findPermissionsByUserId(Long userId);

    List<SysPermission> findAll();

    List<SysPermission> findPermissionsByRoleId(Long roleId);

    SysPermission findById(Long id);

    SysPermission findByCode(String permissionCode);

    int insert(SysPermission permission);
}
