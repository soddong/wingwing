package com.ssafy.shieldroneapp.data.model

/**
 * 인증 토큰 정보를 나타내는 데이터 클래스
 *
 * @property accessToken 액세스 토큰
 * @property refreshToken 리프레시 토큰
 */
data class Tokens(
    val accessToken: String,
    val refreshToken: String
)