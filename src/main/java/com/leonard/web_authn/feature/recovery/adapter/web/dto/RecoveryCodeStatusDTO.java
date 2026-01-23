package com.leonard.web_authn.feature.recovery.adapter.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RecoveryCodeStatusDTO(int totalCodes, int usedCodes, int remainingCodes) {}
