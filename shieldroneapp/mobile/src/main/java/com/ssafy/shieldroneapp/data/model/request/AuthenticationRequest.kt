package com.ssafy.shieldroneapp.data.model.request

import com.ssafy.shieldroneapp.data.model.User

/**
 * 1. 인증 코드 전송 요청 시 서버로 전송하는 데이터 클래스
 *
 * @property phoneNumber 사용자의 전화번호
 */
data class VerificationCodeRequest(
    val phoneNumber: String
)

/**
 * 2. 인증 코드 검증 요청 시 서버로 전송하는 데이터 클래스
 *
 * @property phoneNumber 사용자의 전화번호
 * @property authCode 사용자 입력 인증 코드
 */
data class CodeVerificationRequest(
    val phoneNumber: String,
    val authCode: String
)

/**
 * 3. 회원가입 요청 시 서버로 전송하는 사용자 데이터 클래스
 *
 *
 * 인증 과정에서 수집(ViewModel)된 임시 사용자 입력 데이터를 기반으로
 * 회원가입을 진행합니다.
 * @property username 사용자 이름
 * @property birthday 생년월일 (YYYY-MM-DD 형식)
 * @property phoneNumber 핸드폰 번호
 */
data class UserAuthRequest(
    val username: String = "",
    val birthday: String = "",  // 생년월일 (YYYY-MM-DD 형식)
    val phoneNumber: String = "",
) {
    /**
     * UserAuthData => User 모델로 변환
     * 인증 과정에서 수집된 기본 정보 만으로 User 객체 생성
     *
     * @return 기본 정보가 설정된 User 객체
     * */
    fun toUser() = User(
        username = username,
        birthday = birthday,
        phoneNumber = phoneNumber
    )
}

/**
 * 4. 로그인 요청 시 서버로 전송하는 전화번호 데이터 클래스
 *
 * 본인 인증 완료 후, 전화번호를 통해 로그인을 요청하는 데 사용됩니다.
 * @property phoneNumber 사용자의 전화번호
 */
data class PhoneNumberRequest(
    val phoneNumber: String
)

/**
 * 5. 리프레시 토큰 재발급 요청 시 서버로 전송하는 리프레시 토큰 데이터 클래스
 *
 * 액세스 토큰이 만료된 경우, 저장된 리프레시 토큰을 통해 새로운 액세스 토큰을 요청하는 데 사용됩니다.
 * @property refreshToken 저장된 리프레시 토큰
 */
data class TokenRequest(
    val refreshToken: String
)