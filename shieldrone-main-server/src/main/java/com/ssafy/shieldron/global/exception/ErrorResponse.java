package com.ssafy.shieldron.global.exception;

public record ErrorResponse (
        String code,
        String message
) {
}
