package com.ssafy.shieldroneapp.ui.components

/**
 * 다양한 상황에 맞는 Alert 컴포넌트를 제공하는 클래스.
 *
 * 첫 번째 타입: 드론 배정 성공/실패/취소, 경로 안내 종료 시 나타나는 일반적인 알림.
 * 두 번째 타입: 위험 상황 감지 시 나타나는 알림으로, 시간 정보와 위험 수준에 따른 이미지가 포함된다.
 *   버튼이 있는 경우와 없는 경우로 나뉘며, 3단계 위험 시 "알림을 보내지 않겠습니다" 버튼이 표시된다.
 *
 * @param title 알림의 제목
 * @param content 알림의 상세 내용
 * @param time 위험 상황 감지 시 표시할 시간 정보 (두 번쨰 타입만)
 * @param dangerLevelImage 위험 수준에 따른 이미지 (두 번쨰 타입만)
 * @param hasButton 버튼 포함 여부. 위험 상황 3단계 시 "알림을 보내지 않겠습니다" 버튼이 표시됨.
 * @param onButtonClick 버튼 클릭 시 호출되는 콜백 함수 (필요할 경우)
 */