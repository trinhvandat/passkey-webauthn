package com.leonard.web_authn.feature.authorization.adapter.web.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RoleInfoDTO {
    private Integer id;
    private String name;
    private String displayName;
    private String description;
    private Boolean isSystem;
}
