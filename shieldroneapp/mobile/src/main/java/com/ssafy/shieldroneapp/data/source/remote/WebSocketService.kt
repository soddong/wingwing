package com.ssafy.shieldroneapp.data.source.remote

import android.content.Context
import android.util.Log
import com.ssafy.shieldroneapp.data.model.AudioData
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.source.local.AudioDataLocalSource
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
    private val webSocketMessageSender: WebSocketMessageSender,
    private val audioDataLocalSource: AudioDataLocalSource
) {
    companion object {
        private const val TAG = "모바일: 웹소켓 서비스"
        private const val MIN_AUDIO_PROCESS_INTERVAL = 1000L
        private var lastProcessedAudioTime: Long = 0
    }
    private var webSocketConnectionManager: WebSocketConnectionManager? = null
    private val errorHandler = WebSocketErrorHandler(context)
    private val recordingScope = CoroutineScope(Dispatchers.IO + Job())
    private var audioRecorderCallback: ((WebSocketState) -> Unit)? = null

    private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    fun setConnectionManager(manager: WebSocketConnectionManager) {
        webSocketConnectionManager = manager
    }

    fun updateConnectionState(state: WebSocketState) {
        _connectionState.value = state
        audioRecorderCallback?.invoke(state)
    }

    fun setAudioRecorderCallback(callback: (WebSocketState) -> Unit) {
        audioRecorderCallback = callback
    }

    fun getConnectionState(): Boolean {
        return webSocketMessageSender.getWebSocket() != null
    }

    // WebSocket 초기화 및 연결
    fun initialize() {
        try {
            Log.d(TAG, "WebSocket 초기화 시작")
            webSocketConnectionManager?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket 초기화 중 에러 발생", e)
            updateConnectionState(WebSocketState.Error(e))
        }
    }

    // WebSocket 연결 해제
    fun shutdown() {
        try {
            Log.d(TAG, "WebSocket 서비스 종료 시작")
            webSocketConnectionManager?.disconnect()
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
            return
        }

        try {
            webSocketMessageSender.sendWatchSensorData(data)
            Log.d(TAG, "심박수 데이터 전송 성공")
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 전송 실패", e)
            errorHandler.handleMessageError(e)
        }
    }

    fun sendAudioData(audioData: AudioData) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastProcessedAudioTime < MIN_AUDIO_PROCESS_INTERVAL && !audioData.dbFlag) {
            Log.d(TAG, "최소 처리 간격이 지나지 않았고 dbFlag가 false여서 스킵")
            return
        }

        // 수신한 데이터를 먼저 로컬에 저장
        recordingScope.launch {
            try {
                audioDataLocalSource.saveAudioData(audioData)
            } catch (e: Exception) {
                Log.e(TAG, "로컬 저장 실패", e)
            }
        }

        if (!getConnectionState()) {
            Log.d(TAG, "WebSocket 연결되지 않음")
            return
        }

        try {
            webSocketMessageSender.sendAudioData(audioData)
            lastProcessedAudioTime = currentTime
            Log.d(TAG, "음성 분석 데이터 전송 성공")
        } catch (e: Exception) {
            Log.e(TAG, "음성 분석 데이터 전송 실패", e)
            errorHandler.handleMessageError(e)
        }
    }
}