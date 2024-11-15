package com.ssafy.shieldroneapp.data.source.local

import android.content.Context
import android.util.Log
import com.ssafy.shieldroneapp.data.model.HeartRateData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HeartRateLocalDataSource @Inject constructor(@ApplicationContext context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences("heart_rate_data", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "모바일: 로컬 심박수 데이터 소스"
        private const val KEY_PULSE_FLAG = "pulseFlag"
        private const val KEY_BPM = "bpm" 
        private const val KEY_TIMESTAMP = "timestamp"
    }

    private var lastValidBpm: Double = 0.0  // 마지막으로 유효한 심박수 값을 메모리에 저장

    suspend fun saveHeartRateData(data: HeartRateData) {
        withContext(Dispatchers.IO) {
            try {
                if (data.bpm > 0) {
                    lastValidBpm = data.bpm
                    val editor = sharedPreferences.edit()
                    editor.putBoolean(KEY_PULSE_FLAG, data.pulseFlag)
                    editor.putFloat(KEY_BPM, data.bpm.toFloat())
                    editor.putLong(KEY_TIMESTAMP, data.timestamp)
                    editor.apply()
                    Log.d(TAG, "최신 심박수 데이터로 업데이트: $data")
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "로컬 심박수 데이터 저장 중 오류 발생", e)
            }
        }
    }

    suspend fun getHeartRateData(): HeartRateData? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = sharedPreferences.getLong(KEY_TIMESTAMP, 0L)
                if (timestamp != 0L) {
                    val pulseFlag = sharedPreferences.getBoolean(KEY_PULSE_FLAG, false)
                    val storedBpm = sharedPreferences.getFloat(KEY_BPM, 0f).toDouble()
                    // 저장된 bpm이 0이면 마지막 유효값 사용
                    val bpm = if (storedBpm > 0) storedBpm else lastValidBpm
                    HeartRateData(pulseFlag, bpm, timestamp).also {
                        Log.d(TAG, "저장된 심박수 데이터: $it")
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "로컬 심박수 데이터 불러오기 중 오류 발생", e)
                null
            }
        }
    }

    // TODO: 앱 사용 종료나 로그아웃 시에 사용
    suspend fun clearHeartRateData() {
        withContext(Dispatchers.IO) {
            try {
                sharedPreferences.edit().clear().apply()
                lastValidBpm = 0.0
                Log.d(TAG, "로컬 심박수 데이터 초기화 완료")
            } catch (e: Exception) {
                Log.e(TAG, "로컬 심박수 데이터 초기화 중 오류 발생", e)
            }
        }
    }
}