package com.ssafy.shieldroneapp.ui.components

/**
 * 앱 전반에서 사용하는 공통 버튼 컴포넌트.
 *
 * 다양한 버튼 스타일을 제공하며, 액션 버튼으로 사용된다.
 * 버튼 타입에 따라 크기와 색상이 달라진다.
 *
 * FULL_WIDTH: 화면 너비를 꽉 채우는 고정 높이 버튼. 주요 액션에 사용.
 * LARGE: 고정 높이와 80% 너비를 가진 버튼. 주로 "시작하기", "인증하기" 등의 액션에 사용.
 * SMALL: 고정된 높이의 작은 버튼. "설정" 또는 "등록" 등의 짧은 액션에 사용.
 * MODAL_LARGE: 모달 내부에서 사용되는 큰 버튼. 위험 알림 관련 버튼에 사용.
 * MODAL_MEDIUM: 모달 내부에서 사용되는 중간 크기 버튼. 출발지 선택 등에 사용.
 *
 * 버튼 상태는 기본, 비활성화(Disabled)로 나뉘며, 각각의 색상과 스타일을 정의
 * 클릭 이벤트를 처리하여 원하는 동작을 수행할 수 있다.
 *
 * @param text 버튼에 표시할 텍스트
 * @param onClick 버튼 클릭 시 호출되는 콜백 함수
 * @param type 버튼의 스타일을 결정하는 타입 (FULL_WIDTH, LARGE, SMALL, MODAL_LARGE, MODAL_MEDIUM)
 * @param isEnabled 버튼의 활성화 상태. 비활성화 시 회색으로 표시
 */