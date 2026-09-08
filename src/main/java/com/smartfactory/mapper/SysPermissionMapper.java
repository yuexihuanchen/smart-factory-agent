package com.smartfactory.mapper;

import com.smartfactory.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysPermissionMapper {

    List<SysPermission> findPermissionsByUserId(Long userId);
}