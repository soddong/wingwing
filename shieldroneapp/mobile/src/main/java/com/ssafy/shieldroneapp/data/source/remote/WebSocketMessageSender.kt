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

import android.util.Log
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.AudioData
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.model.LatLng
import okhttp3.WebSocket
import okio.ByteString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class WebSocketMessageSender @Inject constructor(private var webSocket: WebSocket?) {

    companion object {
        private const val TAG = "모바일: 웹소켓 메시지 센더"
        private var lastSentDbFlag: Boolean? = null
        private var lastSentLocation: LatLng? = null
        private var lastSentTime: Long = 0
        private val PERIODIC_SEND_INTERVAL = 5000L
        private const val MIN_LOCATION_UPDATE_INTERVAL = 1000L
    }

    fun sendWatchSensorData(data: HeartRateData) {
        try {
            val jsonData = Gson().toJson(mapOf(
                "type" to "sendPulseFlag",
                "time" to data.timestamp,
                "pulseFlag" to data.pulseFlag
            ))
            Log.d(TAG, "전송할 데이터: $jsonData")

            if (webSocket == null) {
                Log.e(TAG, "WebSocket이 null입니다")
                throw Exception("WebSocket이 초기화되지 않았습니다")
            }

            val success = webSocket?.send(jsonData) ?: false
            if (success) {
                Log.d(TAG, "심박수 데이터 전송 성공: $jsonData")
            } else {
                Log.e(TAG, "데이터 전송 실패 - WebSocket 상태: ${webSocket != null}, 데이터: $jsonData")
                if (webSocket != null) {
                    Log.d(TAG, "WebSocket 상태 확인 필요")
                    throw Exception("WebSocket이 연결되어 있지만 전송에 실패했습니다")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 전송 중 에러 발생: ${e.message}", e)
            throw e
        }
    }

    fun sendAudioData(data: AudioData) {
        try {
            requireNotNull(webSocket) { "WebSocket이 null입니다" }

            val currentTime = System.currentTimeMillis()
            if (data.dbFlag ||
                lastSentDbFlag != data.dbFlag ||
                currentTime - lastSentTime >= PERIODIC_SEND_INTERVAL) {

                val jsonData = Gson().toJson(mapOf(
                    "type" to "sendDbFlag",
                    "time" to data.time,
                    "dbFlag" to data.dbFlag
                ))

                Log.d(TAG, "음성 분석 데이터 전송: $jsonData")
                val isSent = webSocket?.send(jsonData) ?: false

                if (isSent) {
                    lastSentDbFlag = data.dbFlag 
                    lastSentTime = currentTime
                    Log.d(TAG, "음성 분석 데이터 전송 성공 - dbFlag: ${data.dbFlag}")
                } else {
                    Log.e(TAG, "데이터 전송 실패")
                    throw Exception("음성 분석 데이터 전송 실패")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "음성 분석 데이터 전송 중 에러 발생", e)
            throw e
        }
    }

    fun getWebSocket(): WebSocket? {
        return webSocket
    }

    fun setWebSocket(socket: WebSocket?) {
        webSocket = socket
        Log.d(TAG, "새로운 WebSocket ${if (socket != null) "설정됨" else "해제됨"}")
    }

    fun sendLocationUpdate(location: LatLng) {
        try {
            requireNotNull(webSocket) { "WebSocket이 null입니다" }

            val currentTime = System.currentTimeMillis()
            // 마지막 전송 위치와 동일하고, 최소 업데이트 간격이 지나지 않은 경우 안보냄
            if (lastSentLocation?.lat == location.lat &&
                lastSentLocation?.lng == location.lng &&
                currentTime - lastSentTime < MIN_LOCATION_UPDATE_INTERVAL) {
                return
            }

            val locationMap = mapOf(
                "type" to "trackPosition",
                "time" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date()),
                "location" to mapOf(
                    "lat" to location.lat.toString(),
                    "lng" to location.lng.toString()
                )
            )

            val jsonData = Gson().toJson(locationMap)
            Log.d(TAG, "위치 데이터 전송: $jsonData")
            val isSent = webSocket?.send(jsonData) ?: false

            if (isSent) {
                lastSentLocation = location
                lastSentTime = currentTime
                Log.d(TAG, "위치 데이터 전송 성공 - lat: ${location.lat}, lng: ${location.lng}")
            } else {
                Log.e(TAG, "위치 데이터 전송 실패")
                throw Exception("위치 데이터 전송 실패")
            }
        } catch (e: Exception) {
            Log.e(TAG, "위치 데이터 전송 중 에러 발생", e)
            throw e
        }
    }
}