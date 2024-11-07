package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KeywordRequest(
        @NotBlank(message = "검색 키워드는 필수입니다.")
        @Size(max = 100, message = "검색 키워드는 최대 100자까지 가능합니다.")
        String keyword
) {
}