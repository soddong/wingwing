package com.ssafy.shieldroneapp.data.source.remote

/**
 * WebSocket을 통해 메시지를 서버에 전송하는 클래스.
 *
 * 실시간 GPS 데이터, 워치 센서 데이터, 위험 상황에 대한 사용자 응답을 서버에 전송
 *
 * 주요 메서드
 * - sendGPSUpdates(): 실시간 GPS 데이터를 서버에 전송
 * - sendWatchSensorData(): 워치 센서 데이터를 서버에 전송
 * - sendDangerResponse(): 3단계 위험 상황에서 응답을 서버에 전송
 *
 * 이 클래스는 WebSocketService에 의해 import됩니다.
 */
