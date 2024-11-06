package com.ssafy.shieldroneapp.data.model

/**
 * 본인 인증 응답 데이터를 나타내는 데이터 클래스.
 *
 * @property isAuthSuccessful 인증 성공 여부
 * @property isAlreadyRegistered 이미 등록된 회원 여부
 */
data class VerificationResponse(
    val isAuthSuccessful: Boolean,
    val isAlreadyRegistered: Boolean
)