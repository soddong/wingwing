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
import okhttp3.WebSocket
import okio.ByteString
import javax.inject.Inject

class WebSocketMessageSender @Inject constructor(private val webSocket: WebSocket?) {

    companion object {
        private const val TAG = "모바일: 웹소켓 메시지 센더"
    }

    fun sendWatchSensorData(data: HeartRateData) {
        val json = data.toJson()
        webSocket?.send(json)
    }

    fun sendAudioData(data: AudioData) {
        try {
            // raw 오디오 데이터를 ByteString으로 변환하여 전송
            val byteString = ByteString.of(*data.audioData)
            webSocket?.send(byteString)

            // 시간 정보는 별도 메타데이터로 전송
            val metadata = mapOf(
                "time" to data.time,
                "type" to "audio"
            ).let { Gson().toJson(it) }

            webSocket?.send(metadata)
        } catch (e: Exception) {
            Log.e(TAG, "오디오 데이터 전송 중 에러 발생", e)
            throw e
        }
    }
}