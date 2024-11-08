package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthCodeVerifyRequest(
        @NotNull @Pattern(regexp = "^010\\d{8}$")
        String phoneNumber,
        @NotNull @Size(min = 6, max = 6)
        String authCode
) {
}
