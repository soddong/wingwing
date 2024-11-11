package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;

public record DroneMatchRequest(
        @NotNull(message = "드론 ID는 필수입니다.")
        Integer droneId,
        @NotNull(message = "드론 CODE는 필수입니다.")
        Integer droneCode
) {
}
