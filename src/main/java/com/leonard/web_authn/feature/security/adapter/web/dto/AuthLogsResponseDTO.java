package com.leonard.web_authn.feature.security.adapter.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuthLogsResponseDTO {
    private List<AuthLogDTO> logs;
    private long total;
    private int page;
    private int size;
}
