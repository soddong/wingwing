package com.ssafy.shieldroneapp.data.model.response

/**
 * 400번 오류 시, 서버로부터 받는 오류 응답 데이터 클래스
 *
 * 번호 인증 및 본인 인증 과정에서 발생할 수 있는 다양한 오류 코드를 포함합니다.
 */
data class ErrorResponse(
    val code: String,
    val message: String
)