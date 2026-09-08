package com.smartfactory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户角色分配请求")
public class UserRoleAssignRequest {

    @NotNull(message = "角色ID列表不能为空")
    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
