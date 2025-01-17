package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;

public record DroneCancelRequest(
        @NotNull(message = "드론 ID는 필수입니다.")
        Integer droneId
) {
}
