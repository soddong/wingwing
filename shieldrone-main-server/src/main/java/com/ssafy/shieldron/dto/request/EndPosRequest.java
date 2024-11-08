package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EndPosRequest(
        @NotBlank(message = "상세 주소를 입력해주세요.")
        @Size(max = 255, message = "상세 주소는 최대 255자까지 가능합니다.")
        String detail,

        @NotNull(message = "위도(latitude)는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
        BigDecimal lat,

        @NotNull(message = "경도(longitude)는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
        BigDecimal lng
) {
}
