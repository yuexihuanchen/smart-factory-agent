package com.smartfactory.security;

import com.smartfactory.entity.SysPermission;
import com.smartfactory.entity.SysRole;
import com.smartfactory.entity.SysUser;
import com.smartfactory.mapper.SysPermissionMapper;
import com.smartfactory.mapper.SysRoleMapper;
import com.smartfactory.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // 1. 查询用户
        SysUser sysUser = sysUserMapper.findByUsername(username);

        if (sysUser == null) {
            throw new UsernameNotFoundException(
                    "用户不存在：" + username
            );
        }

        // 2. 查询角色
        List<SysRole> roles =
                sysRoleMapper.findRolesByUserId(sysUser.getId());

        // 3. 查询权限
        List<SysPermission> permissions =
                sysPermissionMapper.findPermissionsByUserId(
                        sysUser.getId()
                );

        // 4. 转换成 Spring Security 能理解的权限
        List<GrantedAuthority> authorities =
                new ArrayList<>();

        // 角色
        for (SysRole role : roles) {
            authorities.add(
                    new SimpleGrantedAuthority(
                            role.getRoleCode()
                    )
            );
        }

        // 权限
        for (SysPermission permission : permissions) {
            authorities.add(
                    new SimpleGrantedAuthority(
                            permission.getPermissionCode()
                    )
            );
        }

        // 5. 构建 Spring Security 用户
        return User.withUsername(sysUser.getUsername())
                .password(sysUser.getPassword())
                .authorities(authorities)
                .disabled(
                        !"ENABLED".equals(sysUser.getStatus())
                )
                .build();
    }
}