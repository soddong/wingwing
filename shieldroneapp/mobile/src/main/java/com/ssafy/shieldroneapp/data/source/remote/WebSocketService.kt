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
import com.ssafy.shieldroneapp.data.model.AudioData
import com.ssafy.shieldroneapp.data.model.HeartRateData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed class WebSocketState {
    object Connected : WebSocketState()
    object Disconnected : WebSocketState()
    data class Error(val throwable: Throwable) : WebSocketState()
}

class WebSocketService @Inject constructor(
    private val context: Context,
    private val webSocketMessageSender: WebSocketMessageSender
) {
    private var webSocketConnectionManager: WebSocketConnectionManager? = null
    private var isConnected = false
    private val errorHandler = WebSocketErrorHandler(context)
    private val recordingScope = CoroutineScope(Dispatchers.IO + Job())

    // 오디오 레코더 콜백 추가
    private var audioRecorderCallback: ((WebSocketState) -> Unit)? = null

    // 콜백 설정 메서드 추가
    fun setAudioRecorderCallback(callback: (WebSocketState) -> Unit) {
        audioRecorderCallback = callback
    }

    fun getConnectionState(): Boolean {
        val isSocketAvailable = webSocketMessageSender.getWebSocket() != null
        return isConnected && isSocketAvailable
    }

    private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    private companion object {
        const val TAG = "모바일: 웹소켓 서비스"
    }

    fun setConnectionManager(manager: WebSocketConnectionManager) {
        webSocketConnectionManager = manager
    }

    fun updateConnectionState(state: WebSocketState) {
        _connectionState.value = state
        isConnected = (state == WebSocketState.Connected)
        Log.d(TAG, "WebSocket 상태 변경: $state, isConnected: $isConnected, webSocket: ${webSocketMessageSender.getWebSocket() != null}")
        audioRecorderCallback?.invoke(state)
    }

    // WebSocket 초기 설정 및 구독 시작
    fun initialize() {
        try {
            Log.d(TAG, "WebSocket 초기화 시작")
            webSocketConnectionManager?.connect()

            recordingScope.launch {
                // 연결이 완료될 때까지 의도적인 지연
                delay(1000)
                isConnected = webSocketConnectionManager?.isConnected() ?: false
                if (getConnectionState()) {
                    Log.d(TAG, "WebSocket 연결 성공")
                    updateConnectionState(WebSocketState.Connected)
                } else {
                    Log.e(TAG, "WebSocket 연결 실패")
                    updateConnectionState(WebSocketState.Disconnected)
                    handleReconnect()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket 초기화 중 에러 발생", e)
            updateConnectionState(WebSocketState.Error(e))
            handleReconnect()
        }
    }

    // WebSocket 서비스 종료 및 연결 해제
    fun shutdown() {
        try {
            Log.d(TAG, "WebSocket 서비스 종료 시작")
            webSocketConnectionManager?.disconnect()
            isConnected = false
            updateConnectionState(WebSocketState.Disconnected)
            Log.d(TAG, "WebSocket 서비스 종료 완료")
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket 종료 중 에러 발생", e)
            errorHandler.handleConnectionError(e)
        }
    }

    fun sendHeartRateData(data: HeartRateData) {
        if (!getConnectionState()) {
            Log.d(TAG, "WebSocket 연결되지 않음")
            errorHandler.handleErrorEvent("WebSocket이 연결되어 있지 않음")
            handleReconnect()
            // 재연결 후 데이터 재전송 시도
            recordingScope.launch {
                delay(1500) // 재연결 대기
                if (getConnectionState()) {
                    try {
                        webSocketMessageSender.sendWatchSensorData(data)
                        Log.d(TAG, "재연결 후 심박수 데이터 전송 성공")
                    } catch (e: Exception) {
                        Log.e(TAG, "재연결 후 심박수 데이터 전송 실패", e)
                        errorHandler.handleMessageError(e)
                    }
                }
            }
            return
        }

        try {
            Log.d(TAG, "심박수 데이터 전송 시도")
            webSocketMessageSender.sendWatchSensorData(data)
            Log.d(TAG, "심박수 데이터 전송 성공")
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 전송 실패", e)
            errorHandler.handleMessageError(e)
            // 연결이 끊어진 경우에만 재연결 시도
            if (!getConnectionState()) {
                handleReconnect()
            }
        }
    }

    fun sendAudioData(audioData: AudioData) {
        if (!getConnectionState()) {
            Log.d(TAG, "WebSocket 연결되지 않음 - 재연결 시도")
            handleReconnect()
            return
        }

        try {
            Log.d(TAG, "음성 분석 데이터 전송 시도 - dbFlag: ${audioData.dbFlag}")
            webSocketMessageSender.sendAudioData(audioData)
            Log.d(TAG, "음성 분석 데이터 전송 성공")
        } catch (e: Exception) {
            Log.e(TAG, "음성 분석 데이터 전송 실패", e)
            errorHandler.handleMessageError(e)
            handleReconnect()
        }
    }

    fun handleReconnect() {
        Log.d(TAG, "WebSocket 재연결 시도 중...")
        try {
            shutdown()
            // 약간의 지연을 주어 이전 연결이 완전히 종료되도록
            recordingScope.launch {
                kotlinx.coroutines.delay(1000)
                initialize()
            }
        } catch (e: Exception) {
            Log.e(TAG, "재연결 중 에러 발생", e)
            errorHandler.handleConnectionError(e)
            updateConnectionState(WebSocketState.Error(e))
        }
    }
}