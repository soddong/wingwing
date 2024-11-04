package com.ssafy.shieldroneapp.data.source.remote

/**
 * WebSocket 에러를 처리하는 클래스.
 *
 * WebSocket 통신 중 발생하는 오류를 관리
 *
 * 주요 메서드
 * - handleConnectionError(): 연결 관련 오류 처리
 * - handleMessageError(): 메시지 처리 중 발생한 오류 처리
 * - handleErrorEvent(): WebSocket 오류 이벤트 처리
 *
 * 이 클래스는 WebSocketService에 의해 import됩니다.
 * 또한, WebSocketConnectionManager와 협력하여 재연결 및 오류 처리를 수행합니다.
 */