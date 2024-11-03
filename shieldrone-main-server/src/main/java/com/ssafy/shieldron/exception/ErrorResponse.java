package com.ssafy.shieldron.exception;

public record ErrorResponse (
        String code,
        String message
) {
}
