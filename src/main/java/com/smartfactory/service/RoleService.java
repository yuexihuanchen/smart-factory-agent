package com.smartfactory.service;

import com.smartfactory.dto.RoleCreateRequest;
import com.smartfactory.dto.RoleUpdateRequest;
import com.smartfactory.vo.RoleVO;

import java.util.List;

public interface RoleService {

    List<RoleVO> findAll();

    RoleVO findById(Long id);

    RoleVO create(RoleCreateRequest request);

    RoleVO update(Long id, RoleUpdateRequest request);

    void deleteById(Long id);

    RoleVO assignPermissions(Long id, List<Long> permissionIds);
}
