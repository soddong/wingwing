package com.ssafy.shieldron.dto.response;

import java.math.BigDecimal;

public record EndPosResponse(
        String detail,
        BigDecimal lat,
        BigDecimal lng,
        int distance
) {
}
