package com.ssafy.shieldroneapp.data.source.remote

/**
 * WebSocket 연결을 관리하는 클래스.
 *
 * WebSocket 연결 설정 및 해제, 연결 상태를 확인하는 기능을 제공
 *
 * 주요 메서드
 * - connect(): WebSocket 서버에 연결
 * - disconnect(): WebSocket 연결을 해제
 * - isConnected(): WebSocket 연결 상태를 반환
 * - handleReconnect(): 연결 끊김 시 재연결 처리
 *
 * 이 클래스는 WebSocketService에 의해 import됩니다.
 *
 * 추가적으로 다음 클래스들과 협력합니다.
 * - WebSocketConfig: 설정 값을 가져와 연결 옵션(예: 재연결 간격, 타임아웃)을 설정
 * - WebSocketErrorHandler: 연결 중 발생하는 오류를 처리
 */