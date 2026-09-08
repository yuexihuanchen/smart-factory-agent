package com.smartfactory.service.impl;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.dto.UserCreateRequest;
import com.smartfactory.entity.SysUser;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

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
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(mapper, encoder);

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("alice");
        request.setPassword("secret1");
        request.setNickname("Alice");

        when(mapper.findByUsername("alice")).thenReturn(null);
        when(encoder.encode("secret1")).thenReturn("encoded-hash");

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
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(mapper, encoder);

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
}
