package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;


public record SignUpRequest (
        @NotNull
        @Size(min = 2, max = 20)
        @Pattern(regexp = "^[a-zA-Z가-힣]+$")
        String username,
        @NotNull
        LocalDateTime birthday,
        @NotNull
        @Pattern(regexp = "^010\\d{8}$")
        String phoneNumber
) {
}
