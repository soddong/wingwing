package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;

public record DroneAssignmentRequest(
        @NotNull(message = "출발 위치 정보는 필수입니다.")
        LocationRequest startLocation,

        @NotNull(message = "도착 위치 정보는 필수입니다.")
        LocationRequest endLocation
) {
}
