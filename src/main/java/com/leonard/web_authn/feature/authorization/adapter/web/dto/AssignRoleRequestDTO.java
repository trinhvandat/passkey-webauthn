package com.leonard.web_authn.feature.authorization.adapter.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AssignRoleRequestDTO {
    @NotNull
    private Integer roleId;
}
