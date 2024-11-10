package com.ssafy.shieldroneapp.data.source.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.Response
import java.net.SocketException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

class WebSocketConnectionManager(
    private val webSocketService: WebSocketService,
    private val webSocketMessageSender: WebSocketMessageSender,
    private val errorHandler: WebSocketErrorHandler,
    private val webSocketSubscriptions: WebSocketSubscriptions
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private var webSocket: WebSocket? = null
    private val isReconnecting = AtomicBoolean(false)
    private val reconnectScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "웹소켓 연결 매니저"
    }

    fun connect() {
        try {
            Log.d(TAG, "WebSocket 연결 시도: ${WebSocketConfig.getWebSocketUrl()}")

            val request = Request.Builder()
                .url(WebSocketConfig.getWebSocketUrl())
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocketMessageSender.setWebSocket(webSocket)
                    isReconnecting.set(false)  // 재연결 플래그 초기화
                    Log.d(TAG, "연결 성공 및 WebSocket 설정 완료")
                    webSocketService.updateConnectionState(WebSocketState.Connected)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "메시지 수신: $text")
                    webSocketSubscriptions.handleIncomingMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(
                        TAG,
                        "연결 실패 - URL: ${WebSocketConfig.getWebSocketUrl()}, 응답: ${response?.message}",
                        t
                    )

                    // Broken pipe 에러인 경우 즉시 재연결 시도
                    if (t is SocketException && t.message?.contains("Broken pipe") == true) {
                        Log.d(TAG, "Broken pipe 에러 감지, 즉시 재연결 시도")
                        handleReconnect()
                    } else {
                        errorHandler.handleConnectionError(t)
                        handleReconnect()
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "연결 해제 예정: $reason")
                    if (reason == "Disconnecting") {
                        webSocket.close(code, reason)
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "웹소켓 연결 종료: $reason")
                    webSocketMessageSender.setWebSocket(null)
                    webSocketService.updateConnectionState(WebSocketState.Disconnected)

                    // 의도적인 종료("Disconnecting")가 아닌 경우에만 재연결 시도
                    if (reason != "Disconnecting") {
                        handleReconnect()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket 연결 시도 중 에러", e)
            errorHandler.handleConnectionError(e)
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnecting")
        webSocket = null
        // 재연결 작업 중지
        reconnectScope.coroutineContext.cancelChildren()
    }

    fun isConnected(): Boolean {
        return webSocket != null
    }

    private fun handleReconnect() {
        if (isReconnecting.compareAndSet(false, true)) {
            reconnectScope.launch {
                try {
                    // 재연결 전에 기존 연결 정리
                    webSocket?.cancel()
                    webSocket = null
                    delay(WebSocketConfig.getReconnectInterval())
                    connect()
                } finally {
                    isReconnecting.set(false)
                }
            }
        } else {
            Log.d(TAG, "재연결이 이미 진행 중입니다.")
        }
    }
}