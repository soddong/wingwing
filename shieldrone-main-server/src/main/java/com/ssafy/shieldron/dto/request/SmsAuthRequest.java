package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SmsAuthRequest(
        @NotNull @Size(max = 15)
        String phoneNumber
) {
}
