package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;

public record DroneAssignmentRequest(
        @NotNull(message = "드론 ID는 필수입니다.")
        Integer droneId,

        @NotNull(message = "도착 위치 정보는 필수입니다.")
        LocationRequest endLocation
) {
}
