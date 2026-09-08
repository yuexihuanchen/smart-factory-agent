package com.smartfactory.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper {

    List<Long> findPermissionIdsByRoleId(Long roleId);

    int insertIgnore(
            @Param("roleId") Long roleId,
            @Param("permissionId") Long permissionId
    );

    int deleteByRoleId(Long roleId);
}
