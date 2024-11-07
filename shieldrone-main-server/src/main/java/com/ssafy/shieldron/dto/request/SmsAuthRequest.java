package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SmsAuthRequest(
        @NotNull @Pattern(regexp = "^010\\d{8}$", message = "전화번호는 형식에 맞지 않습니다.")
        String phoneNumber
) {
}
