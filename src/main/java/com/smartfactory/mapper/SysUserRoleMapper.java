package com.smartfactory.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserRoleMapper {

    List<Long> findRoleIdsByUserId(Long userId);

    int insertIgnore(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId
    );

    int deleteByUserId(Long userId);

    long countUsersByRoleId(Long roleId);
}
