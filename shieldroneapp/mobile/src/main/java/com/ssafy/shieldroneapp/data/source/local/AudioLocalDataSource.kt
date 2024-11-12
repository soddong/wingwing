package com.ssafy.shieldroneapp.data.source.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import com.ssafy.shieldroneapp.data.model.AudioData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream

@Singleton
class AudioDataLocalSource @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val AUDIO_DATA_DIR = "audio_data"
        private const val TAG = "모바일: 오디오 데이터 로컬"
        private const val MAX_DATA_AGE_MS = 5000L // 5초
        private const val CLEANUP_INTERVAL_MS = 1000L // 1초마다 정리
        private const val MIN_STORE_INTERVAL = 1000L // 최소 저장 간격 1초
        private var lastStoredDbFlag: Boolean? = null
        private var lastStoredTime: Long = 0
    }

    private val audioDirectory: File by lazy {
        File(context.filesDir, AUDIO_DATA_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private var lastCleanupTime = 0L

    // 오디오 데이터 저장
    suspend fun saveAudioData(audioData: AudioData) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()

            if (audioData.dbFlag ||
                lastStoredDbFlag != audioData.dbFlag ||
                currentTime - lastStoredTime >= MIN_STORE_INTERVAL) {

                val fileName = "${audioData.time}.audio"
                val file = File(audioDirectory, fileName)

                FileOutputStream(file).use { outputStream ->
                    val jsonData = Gson().toJson(
                        mapOf(
                            "time" to audioData.time,
                            "dbFlag" to audioData.dbFlag
                        )
                    )
                    outputStream.write(jsonData.toByteArray())
                }

                lastStoredDbFlag = audioData.dbFlag
                lastStoredTime = currentTime
                Log.d(TAG, "음성 분석 데이터 저장 성공: $fileName (dbFlag: ${audioData.dbFlag})")

                // 주기적으로 오래된 데이터 정리
                if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
                    cleanupOldData()
                    lastCleanupTime = currentTime
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "음성 분석 데이터 저장 에러", e)
            throw e
        }
    }

    // 저장된 모든 오디오 데이터 가져오기 (최근 5초)
    suspend fun getStoredAudioData(): List<AudioData> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            audioDirectory.listFiles()
                ?.filter { file ->
                    val timestamp = file.name.substringBefore(".").toLong()
                    currentTime - timestamp <= MAX_DATA_AGE_MS
                }
                ?.sortedBy { it.name }
                ?.mapNotNull { file ->
                    try {
                        val jsonData = file.readText()
                        Gson().fromJson(jsonData, AudioData::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "음성 분석 데이터 파일 읽기 실패: ${file.name}", e)
                        null
                    }
                } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "저장된 음성 분석 데이터 읽기 실패", e)
            emptyList()
        }
    }

    // 5초보다 오래된 데이터 자동 삭제
    private suspend fun cleanupOldData() = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            var deletedCount = 0
            audioDirectory.listFiles()?.forEach { file ->
                try {
                    val timestamp = file.name.substringBefore(".").toLong()
                    if (currentTime - timestamp > MAX_DATA_AGE_MS) {
                        if (file.delete()) {
                            deletedCount++
                        } else {
                            Log.w(TAG, "파일 삭제 실패: ${file.name}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "파일 처리 중 오류 발생: ${file.name}", e)
                }
            }
            if (deletedCount > 0) {
                Log.d(TAG, "5초가 지난 파일 ${deletedCount}개 삭제됨")
            } else {

            }
        } catch (e: Exception) {
            Log.e(TAG, "데이터 정리 중 오류 발생", e)
        }
    }
}