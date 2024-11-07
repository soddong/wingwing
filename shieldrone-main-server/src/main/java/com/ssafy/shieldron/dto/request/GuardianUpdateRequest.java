package com.ssafy.shieldron.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuardianUpdateRequest(
        @NotNull(message = "Guardian ID는 필수입니다.")
        Integer id,

        @NotBlank(message = "관계 정보는 필수입니다.")
        @Size(max = 50, message = "관계 정보는 최대 50자까지 가능합니다.")
        String relation,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "전화번호 형식이 잘못되었습니다. 01012345678 형식을 사용하세요.")
        String phoneNumber
) {
}