package com.ssafy.shieldron.dto.response;

public record RefreshResponse(
        String accessToken,
        String refreshToken
) {
}
