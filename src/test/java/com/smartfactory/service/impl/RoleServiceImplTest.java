package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.entity.SysPermission;
import com.smartfactory.entity.SysRole;
import com.smartfactory.mapper.SysPermissionMapper;
import com.smartfactory.mapper.SysRoleMapper;
import com.smartfactory.mapper.SysRolePermissionMapper;
import com.smartfactory.mapper.SysUserRoleMapper;
import com.smartfactory.vo.RoleVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceImplTest {

    @Test
    void assignPermissionsReplacesRolePermissions() {

        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysPermissionMapper permissionMapper =
                mock(SysPermissionMapper.class);
        SysRolePermissionMapper rolePermissionMapper =
                mock(SysRolePermissionMapper.class);
        SysUserRoleMapper userRoleMapper =
                mock(SysUserRoleMapper.class);

        RoleServiceImpl service = new RoleServiceImpl(
                roleMapper,
                permissionMapper,
                rolePermissionMapper,
                userRoleMapper
        );

        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleCode("ROLE_TEST");
        role.setRoleName("Test Role");

        SysPermission read = new SysPermission();
        read.setId(10L);
        read.setPermissionCode("user:read");
        SysPermission create = new SysPermission();
        create.setId(11L);
        create.setPermissionCode("user:create");

        when(roleMapper.findById(1L)).thenReturn(role);
        when(permissionMapper.findById(10L)).thenReturn(read);
        when(permissionMapper.findById(11L)).thenReturn(create);
        when(permissionMapper.findPermissionsByRoleId(1L))
                .thenReturn(List.of(read, create));

        RoleVO vo = service.assignPermissions(
                1L,
                List.of(10L, 11L)
        );

        verify(rolePermissionMapper).deleteByRoleId(1L);
        verify(rolePermissionMapper).insertIgnore(1L, 10L);
        verify(rolePermissionMapper).insertIgnore(1L, 11L);
        assertThat(vo.getPermissions())
                .containsExactly("user:read", "user:create");
    }

    @Test
    void deleteRoleRejectedWhenAssignedToUsers() {

        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysPermissionMapper permissionMapper =
                mock(SysPermissionMapper.class);
        SysRolePermissionMapper rolePermissionMapper =
                mock(SysRolePermissionMapper.class);
        SysUserRoleMapper userRoleMapper =
                mock(SysUserRoleMapper.class);

        RoleServiceImpl service = new RoleServiceImpl(
                roleMapper,
                permissionMapper,
                rolePermissionMapper,
                userRoleMapper
        );

        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleCode("ROLE_ADMIN");

        when(roleMapper.findById(1L)).thenReturn(role);
        when(userRoleMapper.countUsersByRoleId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteById(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40002);

        verify(rolePermissionMapper, never()).deleteByRoleId(1L);
        verify(roleMapper, never()).deleteById(1L);
    }
}
