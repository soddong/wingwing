package com.ssafy.shieldron.dto.request;

import java.math.BigDecimal;

public record GetHivesInfoRequest(
        BigDecimal lat,
        BigDecimal lng
) {
}
