package com.ssafy.shieldroneapp.data.model

/**
 * 보호자 정보를 나타내는 데이터 클래스.
 *
 * @property relation 보호자와의 관계 (예: 아빠, 엄마 등)
 * @property phoneNumber 보호자 연락처
 */

data class Guardian(
    val relation: String,
    val phoneNumber: String,
)