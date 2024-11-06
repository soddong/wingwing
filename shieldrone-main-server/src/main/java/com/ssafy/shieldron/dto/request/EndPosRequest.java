package com.ssafy.shieldron.dto.request;

import java.math.BigDecimal;

public record EndPosRequest(
        String detail,
        BigDecimal lat,
        BigDecimal lng
) {
}
