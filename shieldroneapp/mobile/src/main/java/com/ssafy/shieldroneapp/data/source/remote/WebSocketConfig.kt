package com.ssafy.shieldroneapp.data.source.remote

class WebSocketConfig {
    companion object {
        const val SERVER_URL = "wss://188c-221-165-58-224.ngrok-free.app"
        private const val TIMEOUT = 5000L
        private const val RECONNECT_INTERVAL = 3000L

        fun getWebSocketUrl(): String {
            return SERVER_URL
        }

        fun getReconnectInterval(): Long {
            return RECONNECT_INTERVAL
        }

        fun getTimeout(): Long {
            return TIMEOUT
        }

        // 목적지 도착 시점이나 모바일 앱을 완전 종료했을 때 연결 해제하기
    }
}