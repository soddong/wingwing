package com.ssafy.shieldron.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    AUTH_CODE_EXPIRED("001", "인증 코드가 만료되었습니다."),
    INVALID_AUTH_CODE("002", "유효하지 않은 인증 코드입니다."),
    USER_NOT_FOUND("003", "사용자를 찾을 수 없습니다."),
    SMS_AUTH_REQUIRED("004", "번호 인증이 필요합니다."),
    DUPLICATE_USER("005", "이미 가입된 회원입니다."),
    INVALID_USER("006", "존재하지 않은 유저입니다."),
    INVALID_TOKEN("007", "존재하니 않은 토큰입니다.")
    ;

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
