package com.ssafy.shieldroneapp.data.source.remote

/**
 * WebSocket을 통해 서버로부터 수신하는 알림 및 메시지를 구독하고 처리하는 클래스.
 *
 * 주로 위험 상황 알림을 단계별로 처리하며, 각 단계에 맞는 UI 동작을 수행
 *
 * 주요 메서드
 * - subscribeToDangerAlerts(): 위험 상황 알림을 구독하고 처리
 *
 * 알림 처리 방식:
 * - 1단계, 2단계: 알림을 표시하고 5초 후 자동으로 닫힘
 * - 3단계: 알림을 표시하며, 사용자가 '괜찮습니다. 위험하지 않습니다.'를 선택할 경우,
 *   서버에 응답을 전송하는 로직이 필요 (이 부분은 WebSocketMessageSender에서 처리)
 *
 * 이 클래스는 WebSocketService에 의해 import되어 사용됩니다.
 * 또한 WebSocketMessageParser를 import하여 수신한 메시지를 파싱합니다.
 */


import android.util.Log
import com.google.gson.JsonParser
import com.ssafy.shieldroneapp.services.alert.AlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class WebSocketSubscriptions @Inject constructor(
    private val messageParser: WebSocketMessageParser,
    private val alertService: AlertService
) {
    companion object {
        private const val TAG = "모바일: 웹소켓 구독"
    }

    private val subscriptionScope = CoroutineScope(Dispatchers.IO)

    fun handleIncomingMessage(message: String) {
        subscriptionScope.launch {
            try {
                Log.d(TAG, "수신된 메시지: $message")
                val messageType = messageParser.getMessageType(message)
                Log.d(TAG, "메시지 타입: $messageType")

                when (messageType) {
                    "triggerWarningBeep" -> {
                        val warningData = messageParser.parseWarningMessage(message)
                        if (warningData != null) {
                            Log.d(TAG, "위험 알림 수신 - time: ${warningData.time}, warningFlag: ${warningData.warningFlag}")

                            if (warningData.warningFlag) {
                                Log.d(TAG, "⚠️ 위험 상황 감지! 경고음 발생 필요")
                                alertService.handleWarningBeep(warningData.warningFlag)
                            }
                        }
                    }
                    null -> Log.e(TAG, "메시지 타입을 찾을 수 없음")
                    else -> Log.d(TAG, "처리되지 않은 메시지 타입: $messageType")
                }
            } catch (e: Exception) {
                Log.e(TAG, "메시지 처리 중 오류 발생", e)
                e.printStackTrace()
            }
        }
    }
}