package com.smartfactory.config;

import com.smartfactory.entity.SysPermission;
import com.smartfactory.entity.SysRole;
import com.smartfactory.entity.SysUser;
import com.smartfactory.mapper.SysPermissionMapper;
import com.smartfactory.mapper.SysRoleMapper;
import com.smartfactory.mapper.SysRolePermissionMapper;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysPermissionMapper sysPermissionMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final SysRolePermissionMapper sysRolePermissionMapper;

    private final PasswordEncoder passwordEncoder;

    @Value("${smart-factory.bootstrap.admin.username}")
    private String adminUsername;

    @Value("${smart-factory.bootstrap.admin.password}")
    private String adminPassword;

    @Value("${smart-factory.bootstrap.admin.nickname}")
    private String adminNickname;

    private static final List<Seed> ROLE_SEEDS = List.of(
            new Seed("ROLE_ADMIN", "系统管理员", "拥有全部权限"),
            new Seed("ROLE_MAINTENANCE", "维修人员", "设备与告警维护权限"),
            new Seed("ROLE_OPERATOR", "操作员", "产线操作相关权限")
    );

    private static final List<Seed> PERMISSION_SEEDS = List.of(
            new Seed("device:read", "查看设备", "查询设备列表与状态"),
            new Seed("device:create", "创建设备", "新增设备"),
            new Seed("device:update", "修改设备", "修改设备与状态"),
            new Seed("device:delete", "删除设备", "删除设备"),
            new Seed("alarm:read", "查看告警", "查询告警"),
            new Seed("alarm:handle", "处理告警", "处理告警"),
            new Seed("workorder:create", "创建工作单", "创建工作单"),
            new Seed("workorder:manage", "管理工作单", "管理工作单"),
            new Seed("user:create", "创建用户", "新增系统用户"),
            new Seed("user:read", "查看用户", "查询系统用户"),
            new Seed("user:update", "修改用户", "修改用户资料与状态"),
            new Seed("user:delete", "删除用户", "删除系统用户"),
            new Seed("role:read", "查看角色", "查询角色与权限"),
            new Seed("role:create", "创建角色", "新增角色"),
            new Seed("role:update", "修改角色", "修改角色与权限"),
            new Seed("role:delete", "删除角色", "删除角色"),
            new Seed("permission:read", "查看权限", "查询权限列表")
    );

    @Override
    public void run(String... args) {

        SysUser admin = ensureAdminUser();
        ensureRoles();
        ensurePermissions();
        bindAdminRoleAndPermissions(admin);

        System.out.println(
                "默认管理员初始化完成：" + adminUsername
        );
    }

    private SysUser ensureAdminUser() {

        SysUser admin = sysUserMapper.findByUsername(adminUsername);

        if (admin != null) {
            return admin;
        }

        SysUser user = new SysUser();
        user.setUsername(adminUsername);
        user.setPassword(passwordEncoder.encode(adminPassword));
        user.setNickname(adminNickname);
        user.setStatus("ENABLED");
        sysUserMapper.insert(user);

        return sysUserMapper.findById(user.getId());
    }

    private void ensureRoles() {

        for (Seed seed : ROLE_SEEDS) {
            if (sysRoleMapper.findByCode(seed.code()) == null) {
                SysRole role = new SysRole();
                role.setRoleCode(seed.code());
                role.setRoleName(seed.name());
                role.setDescription(seed.description());
                sysRoleMapper.insert(role);
            }
        }
    }

    private void ensurePermissions() {

        for (Seed seed : PERMISSION_SEEDS) {
            if (sysPermissionMapper.findByCode(seed.code()) == null) {
                SysPermission permission = new SysPermission();
                permission.setPermissionCode(seed.code());
                permission.setPermissionName(seed.name());
                permission.setDescription(seed.description());
                sysPermissionMapper.insert(permission);
            }
        }
    }

    private void bindAdminRoleAndPermissions(SysUser admin) {

        if (admin == null) {
            return;
        }

        SysRole adminRole = sysRoleMapper.findByCode("ROLE_ADMIN");

        if (adminRole == null) {
            return;
        }

        List<Long> roleIds = sysUserRoleMapper.findRoleIdsByUserId(
                admin.getId()
        );

        if (!roleIds.contains(adminRole.getId())) {
            sysUserRoleMapper.insertIgnore(
                    admin.getId(),
                    adminRole.getId()
            );
        }

        List<SysPermission> permissions = sysPermissionMapper.findAll();

        for (SysPermission permission : permissions) {
            sysRolePermissionMapper.insertIgnore(
                    adminRole.getId(),
                    permission.getId()
            );
        }
    }

    private record Seed(String code, String name, String description) {
    }
}
