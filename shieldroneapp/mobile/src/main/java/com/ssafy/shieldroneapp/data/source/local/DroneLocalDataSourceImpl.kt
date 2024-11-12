package com.ssafy.shieldroneapp.data.source.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.DroneState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DroneLocalDataSourceImpl @Inject constructor(
    context: Context,
    private val gson: Gson
) : DroneLocalDataSource {

    /**
     * EncryptedSharedPreferences 설정으로 보안 강화
     * */
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        "drone_encrypted_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * 1. 드론 상태 저장
     *
     * @param state 로컬에 저장할 드론 상태 객체
     */
    override suspend fun saveDroneState(state: DroneState) {
        val droneStateJson = gson.toJson(state)
        sharedPreferences.edit().putString("droneState", droneStateJson).apply()
    }

    /**
     * 2. 드론 상태 불러오기
     *
     * @return 저장된 드론 상태 객체, 없을 경우 null 반환
     */
    override suspend fun getDroneState(): DroneState? {
        val droneStateJson = sharedPreferences.getString("droneState", null) ?: return null
        return gson.fromJson(droneStateJson, DroneState::class.java)
    }

    /**
     * 3. 드론 상태 업데이트
     *
     * 기존 드론 상태 객체를 불러와 주어진 새로운 상태로 업데이트 후 저장합니다.
     *
     * @param newState 업데이트할 드론 상태
     */
    override suspend fun updateDroneState(newState: DroneState) {
        val currentState = getDroneState() ?: DroneState(
            droneId = newState.droneId
        )
        val updatedState = currentState.copy(
            stationIP = newState.stationIP ?: currentState.stationIP,
            isAssigned = newState.isAssigned,
            isMatched = newState.isMatched,
            estimatedTime = newState.estimatedTime ?: currentState.estimatedTime,
            distance = newState.distance ?: currentState.distance,
            assignedTime = newState.assignedTime ?: currentState.assignedTime
        )
        saveDroneState(updatedState)
    }

    /**
     * 4. 드론 상태 삭제
     *
     * 로컬 저장소에서 드론 상태를 삭제하여 초기화합니다.
     */
    override suspend fun clearDroneState() {
        sharedPreferences.edit().remove("droneState").apply()
    }

    /**
     * 5. 드론 배정 타이머 시작
     *
     * 현재 시간을 기록하여 배정 시작 시점을 저장합니다.
     */
    override suspend fun startAssignmentTimer() {
        val currentTime = System.currentTimeMillis()
        sharedPreferences.edit().putLong("assignmentStartTime", currentTime).apply()
    }

    /**
     * 6. 드론 배정 만료 여부 확인
     *
     * 배정 시작 후 10분이 경과했는지 확인하여 배정 만료 여부를 반환합니다.
     *
     * @return 배정 만료 여부 (10분 초과 시 true, 미만 시 false)
     */
    override suspend fun checkAssignmentExpiration(): Boolean {
        val startTime = sharedPreferences.getLong("assignmentStartTime", -1)
        return if (startTime == -1L) {
            false
        } else {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - startTime
            elapsedTime >= 10 * 60 * 1000 // 10분(600,000 밀리초) 경과 여부 확인
        }
    }
}
