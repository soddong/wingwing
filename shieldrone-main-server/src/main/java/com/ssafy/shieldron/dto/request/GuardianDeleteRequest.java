package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;

public record GuardianDeleteRequest(
        @NotNull(message = "Guardian ID는 필수입니다.")
        Integer id
) {
}
