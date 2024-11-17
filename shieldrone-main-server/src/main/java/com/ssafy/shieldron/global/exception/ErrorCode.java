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
    INVALID_TOKEN("007", "존재하니 않은 토큰입니다."),
    GUARDIAN_ALREADY_EXISTS("008", "이미 존재하는 보호자입니다."),
    MAX_GUARDIAN_REACHED("009", "보호자 등록 한도를 초과했습니다."),
    INVALID_GUARDIAN("010", "유효하지 않은 보호자입니다."),
    INVALID_HIVE("011", "유효하지 않은 정류장입니다."),
    DRONE_NOT_AVAILABLE("012", "이용 가능한 드론이 없습니다."),
    INVALID_DRONE("013", "유효하지 앟은 드론입니다."),
    USER_ALREADY_HAS_DRONE("014", "이미 매칭된 유저입니다."),
    SAME_START_AND_END_LOCATION("015", "출발지와 도착지가 동일합니다."),
    ALREADY_MATCHED_HIVE("016", "이미 매칭 중인 드론 정류장입니다."),
    ALREADY_MATCHED_DRONE("017", "이미 매칭된 드론입니다."),
    NO_GUARDIAN_FOUND("018", "보호자를 찾을 수 없습니다."),
    INVALID_CODE("019", "입력된 인증코드가 일치하지 않습니다.")
    ;


    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
