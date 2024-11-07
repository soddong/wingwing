package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SignInRequest(
        @NotNull
        @Pattern(regexp = "^010\\d{8}$")
        String phoneNumber
) {
}
