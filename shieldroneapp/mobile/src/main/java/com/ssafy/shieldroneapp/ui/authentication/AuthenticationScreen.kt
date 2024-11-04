package com.ssafy.shieldroneapp.ui.authentication

/**
 * 전체 인증 프로세스를 관리하는 화면.
 *
 * 단계별 화면을 순서대로 호출하며, 사용자가 입력한 데이터를 ViewModel에서 관리한다.
 * `currentStep` 상태에 따라 각 단계 컴포저블을 표시하며,
 * ViewModel을 통해 단계 전환과 데이터 처리를 중앙에서 관리한다.
 *
 * @property viewModel 인증 과정을 관리하는 ViewModel (필요할 경우 추가)
 */