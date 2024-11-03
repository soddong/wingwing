package com.ssafy.shieldron.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    AUTH_CODE_EXPIRED("001", "인증 코드가 만료되었습니다."),
    INVALID_AUTH_CODE("002", "유효하지 않은 인증 코드입니다."),
    USER_NOT_FOUND("003", "사용자를 찾을 수 없습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
