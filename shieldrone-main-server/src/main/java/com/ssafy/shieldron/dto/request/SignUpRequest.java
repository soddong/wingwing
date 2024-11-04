package com.ssafy.shieldron.dto.request;

import java.time.LocalDate;

public record SignUpRequest (
        String username,
        LocalDate birthday,
        String phoneNumber
) {
}
