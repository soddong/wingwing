package com.ssafy.shieldroneapp.data.source.remote

import android.content.Context
import android.util.Log
import com.ssafy.shieldroneapp.R

class WebSocketErrorHandler(private val context: Context) {
  // 연결 관련 오류 처리
    fun handleConnectionError(e: Throwable) {
        val errorMessage = context.getString(R.string.connection_error, e.message)
        Log.e("소켓 연결 오류 발생", errorMessage)
    }

    // 메시지 처리 중 발생한 오류 처리
    fun handleMessageError(e: Throwable) {
        val errorMessage = context.getString(R.string.message_processing_error, e.message)
        Log.e("소켓 메시지 처리 중 에러 발생", errorMessage)
    }

    // WebSocket 오류 이벤트 처리
    fun handleErrorEvent(message: String) {
        val errorMessage = context.getString(R.string.error_event, message)
        Log.e("WebSocket 이벤트 오류 발생", errorMessage)
    }
}