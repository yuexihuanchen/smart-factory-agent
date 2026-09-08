package com.smartfactory.mapper;

import com.smartfactory.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysUserMapper {

    SysUser findByUsername(String username);

    int insert(SysUser user);

    SysUser findById(Long id);

    List<SysUser> findAll();

    int update(SysUser user);

    int deleteById(Long id);
}
