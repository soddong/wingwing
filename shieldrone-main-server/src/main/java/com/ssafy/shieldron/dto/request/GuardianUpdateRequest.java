package com.ssafy.shieldron.dto.request;

public record GuardianUpdateRequest(
        Integer id,
        String relation,
        String phoneNumber
) {
}
