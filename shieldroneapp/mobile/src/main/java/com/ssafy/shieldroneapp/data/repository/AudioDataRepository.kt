package com.ssafy.shieldroneapp.data.repository

import android.util.Log
import com.ssafy.shieldroneapp.data.model.AudioData
import com.ssafy.shieldroneapp.data.source.local.AudioDataLocalSource
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioDataRepository @Inject constructor(
    private val webSocketService: WebSocketService,
    private val localDataSource: AudioDataLocalSource
) {
    companion object {
        private const val TAG = "모바일: 오디오 데이터 레포"
        private const val MIN_PROCESS_INTERVAL = 1000L
    }

    private var lastProcessedDbFlag: Boolean? = null
    private var lastProcessedTime: Long = 0
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private var isSending = false

    suspend fun processAudioData(audioData: AudioData) {
        try {
            val currentTime = System.currentTimeMillis()

            if (audioData.dbFlag ||
                lastProcessedDbFlag != audioData.dbFlag ||
                currentTime - lastProcessedTime >= MIN_PROCESS_INTERVAL) {

                Log.d(TAG, "음성 분석 데이터 처리중 - 시간: ${audioData.time}, dbFlag: ${audioData.dbFlag}")

                try {
                    webSocketService.sendAudioData(audioData)
                    lastProcessedDbFlag = audioData.dbFlag
                    lastProcessedTime = currentTime
                    Log.d(TAG, "웹소켓 전송 성공")
                } catch (e: Exception) {
                    Log.e(TAG, "웹소켓 전송 실패, 로컬에 저장", e)
                    localDataSource.saveAudioData(audioData)
                    startSendingLocalAudioData()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "음성 분석 데이터 처리 실패", e)
            throw e
        }
    }

    fun startSendingLocalAudioData(intervalMillis: Long = 1000L) {
        if (isSending) return

        isSending = true
        coroutineScope.launch {
            try {
                while (isActive && isSending) {
                    val storedData = localDataSource.getStoredAudioData()
                    if (storedData.isNotEmpty()) {
                        Log.d(TAG, "저장된 데이터 ${storedData.size}개 재전송 시도")
                        var shouldContinue = true

                        for (audioData in storedData) {
                            if (!shouldContinue) break

                            try {
                                webSocketService.sendAudioData(audioData)
                                Log.d(TAG, "저장된 데이터 전송 성공 - time: ${audioData.time}")
                            } catch (e: Exception) {
                                Log.e(TAG, "저장된 데이터 전송 실패", e)
                                shouldContinue = false
                            }
                        }
                    }
                    delay(intervalMillis)
                }
            } catch (e: Exception) {
                Log.e(TAG, "데이터 재전송 중 오류", e)
            }
        }
    }

    fun stopSendingData() {
        isSending = false
        coroutineScope.coroutineContext.cancelChildren()
    }
}