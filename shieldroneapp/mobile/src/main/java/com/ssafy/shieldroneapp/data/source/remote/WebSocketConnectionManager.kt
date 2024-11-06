package com.ssafy.shieldroneapp.data.source.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

 class WebSocketConnectionManager(
     private val webSocketService: WebSocketService,
     private val errorHandler: WebSocketErrorHandler
 ) {
     private val client = OkHttpClient()
     private var webSocket: WebSocket? = null
     private val isReconnecting = AtomicBoolean(false)
     private val reconnectScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

     // WebSocket 서버 연결
     fun connect() {
         val request = Request.Builder().url(WebSocketConfig.getWebSocketUrl()).build()
         webSocket = client.newWebSocket(request, object : WebSocketListener() {
             override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                 Log.d("WebSocket", "서버 연결")
                 isReconnecting.set(false)
             }

             override fun onMessage(webSocket: WebSocket, text: String) {
                 Log.d("WebSocket", "메시지 수신: $text")
             }

             override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                 Log.e("WebSocket", "연결 실패", t)
                 errorHandler.handleConnectionError(t)
                 handleReconnect()
             }

             override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                 Log.d("WebSocket", "연결 해제 예정: $reason")
                 webSocket.close(code, reason)
             }

             override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                 Log.d("WebSocket", "연결 해제: $reason")
                 handleReconnect()
             }
         })
     }

     // WebSocket 연결 해제
     fun disconnect() {
         webSocket?.close(1000, "Disconnecting")
         webSocket = null
         // 재연결 작업 중지
         reconnectScope.coroutineContext.cancelChildren()
     }

     // WebSocket 연결 상태 반환
     fun isConnected(): Boolean {
         return webSocket != null
     }

     // 연결 끊김 시 재연결 처리
     private fun handleReconnect() {
         if (isReconnecting.compareAndSet(false, true)) {
             reconnectScope.launch {
                 delay(WebSocketConfig.getReconnectInterval())
                 Log.d("WebSocket", "재연결 시도중...")
                 connect()
             }
         }
     }
 }
 