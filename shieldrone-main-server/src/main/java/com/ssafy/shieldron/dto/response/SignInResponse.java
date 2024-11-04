package com.ssafy.shieldron.dto.response;

public record SignInResponse(
        String accessToken,
        String refreshToken
) {
}
