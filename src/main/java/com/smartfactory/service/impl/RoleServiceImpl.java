package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.dto.RoleCreateRequest;
import com.smartfactory.dto.RoleUpdateRequest;
import com.smartfactory.entity.SysPermission;
import com.smartfactory.entity.SysRole;
import com.smartfactory.mapper.SysPermissionMapper;
import com.smartfactory.mapper.SysRoleMapper;
import com.smartfactory.mapper.SysRolePermissionMapper;
import com.smartfactory.mapper.SysUserRoleMapper;
import com.smartfactory.service.RoleService;
import com.smartfactory.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper sysRoleMapper;

    private final SysPermissionMapper sysPermissionMapper;

    private final SysRolePermissionMapper sysRolePermissionMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<RoleVO> findAll() {

        return sysRoleMapper.findAll()
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public RoleVO findById(Long id) {

        return toVO(getExistingRole(id));
    }

    @Override
    public RoleVO create(RoleCreateRequest request) {

        if (sysRoleMapper.findByCode(request.getRoleCode()) != null) {
            throw new BusinessException(40001, "角色编码已存在");
        }

        if (hasRoleName(request.getRoleName())) {
            throw new BusinessException(40001, "角色名称已存在");
        }

        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        sysRoleMapper.insert(role);

        return toVO(sysRoleMapper.findById(role.getId()));
    }

    @Override
    public RoleVO update(Long id, RoleUpdateRequest request) {

        SysRole existing = getExistingRole(id);

        if (!existing.getRoleCode().equals(request.getRoleCode())
                && sysRoleMapper.findByCode(request.getRoleCode()) != null) {
            throw new BusinessException(40001, "角色编码已存在");
        }

        existing.setRoleName(request.getRoleName());
        existing.setRoleCode(request.getRoleCode());
        existing.setDescription(request.getDescription());
        sysRoleMapper.update(existing);

        return toVO(sysRoleMapper.findById(id));
    }

    @Override
    public void deleteById(Long id) {

        getExistingRole(id);

        if (sysUserRoleMapper.countUsersByRoleId(id) > 0) {
            throw new BusinessException(40002, "角色已分配给用户，不能删除");
        }

        sysRolePermissionMapper.deleteByRoleId(id);
        sysRoleMapper.deleteById(id);
    }

    @Override
    public RoleVO assignPermissions(
            Long id,
            List<Long> permissionIds) {

        getExistingRole(id);

        for (Long permissionId : permissionIds) {
            if (sysPermissionMapper.findById(permissionId) == null) {
                throw new BusinessException(40001, "权限不存在");
            }
        }

        sysRolePermissionMapper.deleteByRoleId(id);

        for (Long permissionId : permissionIds) {
            sysRolePermissionMapper.insertIgnore(id, permissionId);
        }

        return toVO(sysRoleMapper.findById(id));
    }

    private boolean hasRoleName(String roleName) {

        return sysRoleMapper.findAll()
                .stream()
                .anyMatch(role -> role.getRoleName().equals(roleName));
    }

    private SysRole getExistingRole(Long id) {

        SysRole role = sysRoleMapper.findById(id);

        if (role == null) {
            throw new BusinessException(40401, "角色不存在");
        }

        return role;
    }

    private RoleVO toVO(SysRole role) {

        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setDescription(role.getDescription());
        vo.setCreatedAt(role.getCreatedAt());
        vo.setUpdatedAt(role.getUpdatedAt());

        List<String> permissions = sysPermissionMapper
                .findPermissionsByRoleId(role.getId())
                .stream()
                .map(SysPermission::getPermissionCode)
                .toList();

        vo.setPermissions(permissions);
        return vo;
    }
}
