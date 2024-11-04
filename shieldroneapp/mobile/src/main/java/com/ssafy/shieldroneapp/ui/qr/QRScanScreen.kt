package com.ssafy.shieldroneapp.ui.qr

/**
 * QR 코드 인식 화면을 구성하는 Composable 함수.
 *
 * QR 코드를 인식하여 드론 매칭을 완료하는 화면으로, 안내 문구와 QR 인증 버튼을 포함한다.
 *
 * QR 코드 인식이 잘못될 경우, InfoMessage의 첫 번째 타입을 이용해
 * "유효하지 않은 코드"라는 안내 문구를 표시한다.
 *
 * QR 코드 인증이 성공하면 서버에서 응답을 받아 지도 메인 화면으로 전환되며,
 * 드론 매칭 성공 알림이 표시된다.
 *
 * @param viewModel QRScanViewModel 객체를 통해 QR 인식 상태와 로직을 관리한다.
 * @param onQRCodeSuccess QR 코드 인증 성공 시 호출되는 콜백 함수로, 지도 메인 화면으로 전환된다.
 */