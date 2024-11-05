package com.ssafy.shieldroneapp.data.source.remote

/**
 * 서버와의 실시간 WebSocket 통신 서비스를 관리하는 메인 클래스.
 *
 * WebSocket 연결 및 구독을 통합 관리하고, 분리된 클래스들을 호출하여 전체 WebSocket 서비스를 제공
 *
 * 주요 메서드
 * - initialize(): WebSocket 초기 설정 및 구독 시작
 * - shutdown(): WebSocket 서비스 종료 및 연결 해제
 *
 * 이 클래스는 다음 클래스를 import하여 사용합니다.
 * - WebSocketConnectionManager: WebSocket 연결을 설정 및 해제하는 데 사용
 * - WebSocketSubscriptions: 서버에서 수신하는 알림을 구독
 * - WebSocketMessageSender: 서버에 데이터를 전송
 * - WebSocketErrorHandler: WebSocket 통신 중 발생하는 오류를 처리
 *
 * @property webSocketClient: 서버와의 WebSocket 통신 클라이언트 객체
 * @property isConnected: WebSocket 연결 상태를 나타내는 Boolean 값
 */

import android.content.Context
import android.util.Log
import com.ssafy.shieldroneapp.data.model.HeartRateData

class WebSocketService(
    private val context: Context,
    private val webSocketConnectionManager: WebSocketConnectionManager,
    private val webSocketMessageSender: WebSocketMessageSender
) {
    private var isConnected = false
    private val errorHandler = WebSocketErrorHandler(context)

    // WebSocket 초기 설정 및 구독 시작
    fun initialize() {
        try {
            webSocketConnectionManager.connect()
            isConnected = webSocketConnectionManager.isConnected()
            if (isConnected) {
                Log.d("WebSocketService", "WebSocket 연결 성공")
                // webSocketSubscriptions.subscribeToTopics()
            } else {
                errorHandler.handleErrorEvent("WebSocket 연결 실패")
            }
        } catch (e: Exception) {
            errorHandler.handleConnectionError(e)
            isConnected = false
        }
    }

    // WebSocket 서비스 종료 및 연결 해제
    fun shutdown() {
        try {
            webSocketConnectionManager.disconnect()
            isConnected = false
            Log.d("WebSocketService", "WebSocket 서비스 종료")
        } catch (e: Exception) {
            errorHandler.handleConnectionError(e)
        }
    }

    fun sendHeartRateData(data: HeartRateData) {
        if (isConnected) {
            try {
                webSocketMessageSender.sendWatchSensorData(data)
            } catch (e: Exception) {
                errorHandler.handleMessageError(e)
                // 메시지 전송 실패 시 재연결 시도
                handleReconnect()
            }
        } else {
            errorHandler.handleErrorEvent("WebSocket이 연결되어 있지 않음")
            // 연결이 없는 경우 재연결 시도
            handleReconnect()
        }
    }

    fun handleReconnect() {
        Log.d("WebSocketService", "재연결 시도 중...")
        try {
            shutdown() // 기존 연결 해제
            initialize() // 다시 연결 시도
        } catch (e: Exception) {
            errorHandler.handleConnectionError(e)
        }
    }
}