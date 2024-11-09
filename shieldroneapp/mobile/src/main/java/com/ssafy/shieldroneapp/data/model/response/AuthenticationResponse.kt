package com.ssafy.shieldroneapp.data.model.response

/**
 * 1. 본인 인증 성공 시 서버로부터 받는 응답 데이터 클래스
 * */
data class VerificationResponse(
    val isAlreadyRegistered: Boolean
)

/**
 * 2. 서버로부터 받는 인증 토큰 관련 응답 데이터 클래스
 * */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

/**
 * 3. 400번 오류 시 서버로부터 받는 인증 관련 오류 응답 데이터 클래스
 *
 * 번호 인증 및 본인 인증 과정에서 발생할 수 있는 다양한 오류 코드를 포함합니다.
 */
data class AuthenticationErrorResponse(
    val code: String,
    val message: String
)