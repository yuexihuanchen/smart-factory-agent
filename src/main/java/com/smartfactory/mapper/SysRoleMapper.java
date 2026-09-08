package com.smartfactory.mapper;

import com.smartfactory.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysRoleMapper {

    List<SysRole> findRolesByUserId(Long userId);
}