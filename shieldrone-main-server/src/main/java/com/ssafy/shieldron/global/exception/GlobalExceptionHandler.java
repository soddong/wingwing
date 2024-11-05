package com.ssafy.shieldron.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        ErrorResponse errorResponse = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());

        HttpStatus responseCode;
        if (errorCode == ErrorCode.INVALID_TOKEN) {
            responseCode = HttpStatus.FORBIDDEN;
        } else {
            responseCode = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(errorResponse, responseCode);
    }
}
