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
    }
}