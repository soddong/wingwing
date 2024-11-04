package com.ssafy.shieldroneapp.data.repository

/**
 * 알람 데이터를 관리하는 리포지토리 클래스.
 *
 * 서버와의 WebSocket 통신을 통해 실시간으로 알람 데이터를 수신하고,
 * 수신된 알람 데이터를 처리하여 화면에 표시하거나 경고음을 발생시킨다.
 *
 * @property webSocketService 서버와의 실시간 통신을 위한 WebSocket 서비스 객체
 */