package com.ssafy.shieldroneapp.ui.landing

/**
 * 앱의 최초 시작 화면을 구성하는 Composable 함수.
 *
 * 앱의 이름과 설명 문구를 화면에 표시하고, 시작하기 버튼을 통해 인증 과정으로 전환할 수 있다.
 * 사용자가 시작하기 버튼을 누르면 `onStartClick` 콜백을 호출하여
 * authentication의 IntroScreen으로 화면이 전환된다.
 *
 * @param onStartClick 시작하기 버튼 클릭 시 호출되는 콜백 함수로, IntroScreen으로 전환된다.
 */