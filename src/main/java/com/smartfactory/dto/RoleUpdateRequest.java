package com.smartfactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改角色请求")
public class RoleUpdateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64个字符")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Pattern(
            regexp = "[A-Z0-9_]+",
            message = "角色编码只能包含大写字母、数字和下划线"
    )
    private String roleCode;

    @Size(max = 255, message = "角色描述长度不能超过255个字符")
    private String description;
}
