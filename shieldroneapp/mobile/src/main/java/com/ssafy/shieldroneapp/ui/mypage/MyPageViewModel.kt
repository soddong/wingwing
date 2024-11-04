package com.ssafy.shieldroneapp.ui.mypage

/**
 * MyPage 화면의 데이터를 관리하는 ViewModel 클래스.
 *
 * 기본 도착지(집) 및 보호자 정보 리스트 상태 관리 및 CRUD 로직 처리
 * 서버와의 데이터 연동 및 UI 상태 업데이트
 *
 * - getHomeAddress(): 설정된 기본 도착지 정보 조회
 * - addHomeAddress(location: Location): 기본 도착지 정보 추가
 * - updateHomeAddress(location: Location): 기본 도착지 정보 수정
 * - deleteHomeAddress(): 기본 도착지 정보 삭제
 * 
 * - getGuardians(): 등록된 보호자 정보 조회
 * - addGuardian(guardian: Guardian): 보호자 정보 추가
 * - updateGuardian(guardian: Guardian): 보호자 정보 수정
 * - deleteGuardian(guardianId: String): 보호자 정보 삭제
 */