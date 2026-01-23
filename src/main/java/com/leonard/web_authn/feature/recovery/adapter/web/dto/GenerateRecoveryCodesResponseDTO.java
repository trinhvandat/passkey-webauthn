package com.leonard.web_authn.feature.recovery.adapter.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GenerateRecoveryCodesResponseDTO(List<String> codes) {}
