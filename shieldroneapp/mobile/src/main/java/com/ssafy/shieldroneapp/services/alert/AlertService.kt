package com.ssafy.shieldroneapp.services.alert

/**
 * 서버에서 수신한 위험 알람 데이터를 처리하고, 단계별 알림을 표시하며 경고음을 발생시키는 서비스 클래스.
 *
 * WebSocket을 통해 수신한 알람 메시지를 단계별(1~3단계)로 구분하여 알림을 표시한다.
 * 3단계 위험 알람 발생 시, 경고음을 발생시키고 사용자가 알림을 취소할 수 있는 버튼을 제공하여
 * 긴급 상황에서 적절히 대처할 수 있게 한다.
 *
 * @property webSocketService 서버와의 실시간 통신을 위한 WebSocket 서비스 객체
 * @property notificationManager 알람 알림을 관리하는 NotificationManager 객체
 * @property audioManager 경고음을 제어하는 AudioManager 객체
 */