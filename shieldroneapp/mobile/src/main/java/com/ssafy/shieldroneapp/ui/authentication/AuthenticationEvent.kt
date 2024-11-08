package com.ssafy.shieldroneapp.ui.authentication

/**
 * 인증 화면에서 발생하는 이벤트를 정의한 sealed class
 *
 * [NameSubmitted] 이름 입력 제출
 * [BirthSubmitted] 생년월일 입력 제출
 * [PhoneSubmitted] 전화번호 입력 제출
 * [VerificationSubmitted] 인증번호 제출
 * [TermsAccepted] 약관 동의 상태 변경
 * [BackPressed] 뒤로가기 버튼 클릭
 * [NextPressed] 다음 버튼 클릭
 * [ResendVerification] 인증번호 재전송 요청
 * [StartAuthentication] 인증 프로세스 시작
 */
sealed class AuthenticationEvent {
    data class NameSubmitted(val name: String) : AuthenticationEvent()
    data class BirthSubmitted(val birth: String) : AuthenticationEvent()
    data class PhoneSubmitted(val phone: String) : AuthenticationEvent()
    data class VerificationSubmitted(val code: String) : AuthenticationEvent()
    data class TermsAccepted(val accepted: Boolean) : AuthenticationEvent()
    object BackPressed : AuthenticationEvent()
    object NextPressed : AuthenticationEvent()
    object ResendVerification : AuthenticationEvent()
    object StartAuthentication : AuthenticationEvent()
}