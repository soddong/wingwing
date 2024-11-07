package com.ssafy.shieldroneapp.data.model

import com.ssafy.shieldroneapp.utils.Constants

/**
 * 사용자 정보를 나타내는 데이터 클래스.
 *
 * 서버와의 통신 및 로컬 저장소에서 사용되는 사용자의 전체 정보를 포함한다.
 * 인증 과정에서는 기본 정보만 설정되며, 나머지 정보는 추후 설정할 수 있다.
 *
 * @property userId 사용자 ID (서버에서 생성)
 * @property username 사용자 이름
 * @property birthday 주민등록번호 (암호화 필요)
 * @property phoneNumber 핸드폰 번호
 * @property homeAddress 등록된 기본 도착지 (집) 정보
 * @property lat 집 주소의 위도
 * @property lng 집 주소의 경도
 * @property guardians 등록된 보호자 정보 리스트 (최대 3인 / Guardian 모델 참조)
 */
data class User(
    val userId: String = "",
    val username: String,
    val birthday: String,
    val phoneNumber: String,
    val homeAddress: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val guardians: List<Guardian> = emptyList()
) {
    /**
     * 보호자 추가 가능 여부 확인
     *
     * @return 현재 등록된 보호자가 3명 미만이면 true
     */
    fun canAddGuardian(): Boolean =
        guardians.size < Constants.User.MAX_GUARDIANS
}