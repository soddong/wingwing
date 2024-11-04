package com.ssafy.shieldron.dto;

import java.time.LocalDate;

public record SignUpRequest (
        String username,
        LocalDate birthday,
        String phoneNumber
) {
}
