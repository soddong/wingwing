package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;

public record RefreshRequest(
        @NotNull
        String refreshToken
) {
}
