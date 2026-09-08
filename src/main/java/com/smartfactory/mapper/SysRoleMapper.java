package com.smartfactory.mapper;

import com.smartfactory.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysRoleMapper {

    List<SysRole> findRolesByUserId(Long userId);

    List<SysRole> findAll();

    SysRole findById(Long id);

    SysRole findByCode(String roleCode);

    int insert(SysRole role);

    int update(SysRole role);

    int deleteById(Long id);
}
