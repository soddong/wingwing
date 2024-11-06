package com.ssafy.shieldron.dto.request;

import java.math.BigDecimal;

public record LocationRequest(
        Integer hiveId,
        BigDecimal lat,
        BigDecimal lng
) {
}
