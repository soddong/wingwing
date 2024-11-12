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