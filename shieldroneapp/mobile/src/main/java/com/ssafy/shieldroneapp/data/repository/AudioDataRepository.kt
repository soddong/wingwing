package com.ssafy.shieldroneapp.data.repository

import android.util.Log
import com.ssafy.shieldroneapp.data.model.AudioData
import com.ssafy.shieldroneapp.data.source.local.AudioDataLocalSource
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import javax.inject.Inject

class AudioDataRepository @Inject constructor(
    private val webSocketService: WebSocketService,
    private val localDataSource: AudioDataLocalSource
) {
    companion object {
        private const val TAG = "모바일: 오디오 데이터 레포"
    }

    suspend fun processAudioData(audioData: AudioData) {
        try {
            Log.d(TAG, "오디오 데이터 처리중 - 시간: ${audioData.time}, 크기: ${audioData.audioData.size} bytes")
            webSocketService.sendAudioData(audioData)
            Log.d(TAG, "웹소켓 전송 성공")
        } catch (e: Exception) {
            Log.e(TAG, "오디오 데이터 전송 실패, 로컬에 저장", e)
            localDataSource.saveAudioData(audioData)
            Log.d(TAG, "오디오 데이터가 로컬 저장")
        }
    }
}