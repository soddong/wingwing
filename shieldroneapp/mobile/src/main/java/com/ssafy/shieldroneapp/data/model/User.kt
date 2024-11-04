package com.ssafy.shieldroneapp.data.model

/**
 * 사용자 정보를 나타내는 데이터 클래스.
 *
 * @property userId 사용자 ID
 * @property username 사용자 이름
 * @property birthday 주민등록번호
 * @property phoneNumber 핸드폰 번호
 * @property homeAddress 등록된 기본 도착지 (집) 정보
 * @property lat 집 주소의 위도
 * @property lng 집 주소의 경도
 * @property guardians 등록된 보호자 정보 리스트 (최대 3인 / Guardian 모델 참조)
 */