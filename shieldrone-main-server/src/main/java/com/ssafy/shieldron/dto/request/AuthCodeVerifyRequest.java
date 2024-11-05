package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuthCodeVerifyRequest(
        @NotNull @Size(max = 15)
        String phoneNumber,
        @NotNull @Size(max = 4)
        String authCode
) {
}
