package com.ssafy.shieldroneapp.ui.authentication

/**
 * 인증 과정의 상태와 데이터를 관리하는 ViewModel.
 *
 * 각 인증 단계의 상태와 사용자 입력 데이터를 일시적으로 관리하고,
 * 단계 이동 함수 `goToNextStep`으로 인증 흐름을 제어한다.
 *
 * 단계별 입력 데이터는 `mutableStateOf`를 사용해 ViewModel 내에서 임시로 저장되며,
 * 모든 인증 과정이 완료된 후에는 `UserRepository`를 통해 최종적으로
 * 로컬 저장소 또는 서버로 데이터를 전송하여 저장할 수 있다.
 *
 * @property userRepository 사용자 데이터를 저장하고 전송하는 리포지토리 객체
 */