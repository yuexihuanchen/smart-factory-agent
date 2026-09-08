package com.smartfactory.mapper;

import com.smartfactory.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper {

    SysUser findByUsername(String username);

    int insert(SysUser user);

    SysUser findById(Long id);
}