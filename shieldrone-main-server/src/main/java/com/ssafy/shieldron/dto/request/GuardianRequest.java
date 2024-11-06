package com.ssafy.shieldron.dto.request;

public record GuardianRequest(
        // TODO enum 적용 가능?
        String relation,
        String phoneNumber

) {
}
