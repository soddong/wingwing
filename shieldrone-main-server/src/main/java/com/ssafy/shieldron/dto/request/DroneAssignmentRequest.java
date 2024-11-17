package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;

public record DroneAssignmentRequest(
        @NotNull(message = "하이브 ID는 필수입니다.")
        Integer hiveId,

        @NotNull(message = "도착 위치 정보는 필수입니다.")
        LocationRequest endLocation
) {
}
