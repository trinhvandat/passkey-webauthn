package com.leonard.web_authn.feature.authorization.adapter.web.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RevokeRoleRequestDTO {
    private String reason;
}
