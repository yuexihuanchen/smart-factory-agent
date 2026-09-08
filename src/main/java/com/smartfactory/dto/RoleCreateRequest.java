package com.smartfactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建角色请求")
public class RoleCreateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64个字符")
    @Schema(description = "角色名称")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Pattern(
            regexp = "[A-Z0-9_]+",
            message = "角色编码只能包含大写字母、数字和下划线"
    )
    @Schema(description = "角色编码", example = "ROLE_AUDITOR")
    private String roleCode;

    @Size(max = 255, message = "角色描述长度不能超过255个字符")
    @Schema(description = "角色描述")
    private String description;
}
