package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.dto.UserCreateRequest;
import com.smartfactory.entity.SysRole;
import com.smartfactory.entity.SysUser;
import com.smartfactory.mapper.SysPermissionMapper;
import com.smartfactory.mapper.SysRoleMapper;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.mapper.SysUserRoleMapper;
import com.smartfactory.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @Test
    void createEncodesPasswordAndReturnsRefetchedTimestamps() {

        SysUserMapper mapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysPermissionMapper permissionMapper =
                mock(SysPermissionMapper.class);
        SysUserRoleMapper userRoleMapper =
                mock(SysUserRoleMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(
                mapper,
                roleMapper,
                permissionMapper,
                userRoleMapper,
                encoder
        );

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("alice");
        request.setPassword("secret1");
        request.setNickname("Alice");

        when(mapper.findByUsername("alice")).thenReturn(null);
        when(encoder.encode("secret1")).thenReturn("encoded-hash");
        when(roleMapper.findRolesByUserId(7L)).thenReturn(List.of());
        when(permissionMapper.findPermissionsByUserId(7L))
                .thenReturn(List.of());

        SysUser inserted = new SysUser();
        inserted.setId(7L);
        doAnswer(invocation -> {
            SysUser saved = invocation.getArgument(0);
            saved.setId(7L);
            return 1;
        }).when(mapper).insert(any(SysUser.class));
        when(mapper.findById(7L)).thenReturn(inserted);

        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 8, 10, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 8, 10, 0, 1);
        inserted.setUsername("alice");
        inserted.setNickname("Alice");
        inserted.setStatus("ENABLED");
        inserted.setCreatedAt(createdAt);
        inserted.setUpdatedAt(updatedAt);

        UserVO vo = service.create(request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-hash");
        assertThat(captor.getValue().getStatus()).isEqualTo("ENABLED");
        assertThat(vo.getId()).isEqualTo(7L);
        assertThat(vo.getCreatedAt()).isEqualTo(createdAt);
        assertThat(vo.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void createRejectsDuplicateUsernameBeforeEncoding() {

        SysUserMapper mapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysPermissionMapper permissionMapper =
                mock(SysPermissionMapper.class);
        SysUserRoleMapper userRoleMapper =
                mock(SysUserRoleMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(
                mapper,
                roleMapper,
                permissionMapper,
                userRoleMapper,
                encoder
        );

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("maintenance");
        request.setPassword("123456");
        request.setNickname("Maintenance");

        when(mapper.findByUsername("maintenance"))
                .thenReturn(new SysUser());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40001);

        verify(mapper, never()).insert(any(SysUser.class));
        verify(encoder, never()).encode(any());
    }

    @Test
    void assignRolesReplacesExistingUserRoles() {

        SysUserMapper mapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysPermissionMapper permissionMapper =
                mock(SysPermissionMapper.class);
        SysUserRoleMapper userRoleMapper =
                mock(SysUserRoleMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(
                mapper,
                roleMapper,
                permissionMapper,
                userRoleMapper,
                encoder
        );

        SysUser user = new SysUser();
        user.setId(5L);
        user.setUsername("operator");
        user.setStatus("ENABLED");

        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleCode("ROLE_OPERATOR");

        when(mapper.findById(5L)).thenReturn(user);
        when(roleMapper.findById(2L)).thenReturn(role);
        when(roleMapper.findRolesByUserId(5L))
                .thenReturn(List.of(role));
        when(permissionMapper.findPermissionsByUserId(5L))
                .thenReturn(List.of());

        UserVO vo = service.assignRoles(5L, List.of(2L));

        verify(userRoleMapper).deleteByUserId(5L);
        verify(userRoleMapper).insertIgnore(5L, 2L);
        assertThat(vo.getRoles()).containsExactly("ROLE_OPERATOR");
    }
}
