package com.ssafy.shieldroneapp.data.source.local

import android.content.Context
import android.util.Log
import com.ssafy.shieldroneapp.data.model.HeartRateData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HeartRateLocalDataSource @Inject constructor(@ApplicationContext context: Context) {

    private val sharedPreferences = context.getSharedPreferences("heart_rate_data", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "모바일: 로컬 심박수 데이터 소스"
    }

    suspend fun saveHeartRateData(data: HeartRateData) {
        withContext(Dispatchers.IO) {
            try {
                val editor = sharedPreferences.edit()
                editor.putBoolean("pulseFlag", data.pulseFlag)
                editor.putLong("timestamp", data.timestamp)
                editor.apply()
                Log.d(TAG, "심박수 데이터가 로컬에 저장되었습니다: $data")
            } catch (e: Exception) {
                Log.e(TAG, "로컬 심박수 데이터 저장 중 오류 발생", e)
            }
        }
    }

    // 로컬에 저장된 심박수 데이터를 불러오는 함수
    suspend fun getHeartRateData(): HeartRateData? {
        return withContext(Dispatchers.IO) {
            try {
                val pulseFlag = sharedPreferences.getBoolean("pulseFlag", false)
                val timestamp = sharedPreferences.getLong("timestamp", 0L)

                if (timestamp != 0L) {
                    val heartRateData = HeartRateData(pulseFlag, timestamp)
                    Log.d(TAG, "로컬에서 심박수 데이터 불러옴: $heartRateData")
                    heartRateData
                } else {
                    Log.d(TAG, "로컬에 저장된 심박수 데이터가 없습니다.")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "로컬 심박수 데이터 불러오기 중 오류 발생", e)
                null
            }
        }
    }

    // 로컬에 저장된 심박수 데이터를 삭제하는 함수
    // TODO: 앱 사용 종료나 로그아웃 시에 사용
    suspend fun clearHeartRateData() {
        withContext(Dispatchers.IO) {
            try {
                sharedPreferences.edit().clear().apply()
                Log.d(TAG, "로컬 심박수 데이터가 삭제되었습니다.")
            } catch (e: Exception) {
                Log.e(TAG, "로컬 심박수 데이터 삭제 중 오류 발생", e)
            }
        }
    }
}