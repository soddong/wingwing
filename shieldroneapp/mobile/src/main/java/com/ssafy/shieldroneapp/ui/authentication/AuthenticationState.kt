package com.ssafy.shieldroneapp.ui.authentication

/**
 * 인증 프로세스의 데이터 상태를 관리하는 데이터 클래스
 *
 * @property currentStep 현재 인증 단계
 * @property username 사용자 이름
 * @property birthday 생년월일
 * @property phoneNumber 전화번호
 * @property isLoading 로딩 상태
 * @property error 에러 메시지
 * @property isVerificationSent 인증번호 전송 여부
 * @property isVerified 인증 완료 여부
 * @property isTermsAccepted 약관 동의 여부
 */
data class AuthenticationState(
    val currentStep: AuthStep = AuthStep.Intro,
    val username: String = "",
    val birthday: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVerificationSent: Boolean = false,
    val isVerified: Boolean = false,
    val isTermsAccepted: Boolean = false
)