package com.smartfactory.config;

import com.smartfactory.entity.SysUser;
import com.smartfactory.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${smart-factory.bootstrap.admin.username}")
    private String adminUsername;

    @Value("${smart-factory.bootstrap.admin.password}")
    private String adminPassword;

    @Value("${smart-factory.bootstrap.admin.nickname}")
    private String adminNickname;

    @Override
    public void run(String... args) {

        SysUser admin =
                sysUserMapper.findByUsername(adminUsername);

        if (admin != null) {
            return;
        }

        SysUser user = new SysUser();

        user.setUsername(adminUsername);

        user.setPassword(
                passwordEncoder.encode(adminPassword)
        );

        user.setNickname(adminNickname);

        user.setStatus("ENABLED");

        sysUserMapper.insert(user);

        System.out.println(
                "默认管理员初始化完成：" + adminUsername
        );
    }
}