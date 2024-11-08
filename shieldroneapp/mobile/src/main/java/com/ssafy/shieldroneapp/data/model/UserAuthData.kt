package com.ssafy.shieldroneapp.data.model

/**
 * 인증 과정에서 수집되는 임시 사용자 데이터
 *
 * ViewModel에서 임시로 사용자 입력을 저장하고,
 * 인증 완료 시 User 모델로 변환하여 서버에 전송한다.
 * 
 * @property username 사용자 이름
 *  @property birthday 생년월일 (YYYY-MM-DD 형식)
 * @property phoneNumber 핸드폰 번호
 * @property isTermsAccepted 이용약관 동의 여부
 */

data class UserAuthData(
    val username: String = "",
    val birthday: String = "",  // 생년월일 (YYYY-MM-DD 형식)
    val phoneNumber: String = "",
    val isTermsAccepted: Boolean = false
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