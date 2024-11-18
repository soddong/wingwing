package com.shieldrone.station.model

import android.util.Log
import androidx.lifecycle.ViewModel
import dji.sdk.keyvalue.key.GimbalKey.KeyConnection
import dji.sdk.keyvalue.key.GimbalKey.KeyGimbalAttitude
import dji.sdk.keyvalue.key.GimbalKey.KeyGimbalReset
import dji.sdk.keyvalue.key.GimbalKey.KeyRotateByAngle
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode
import dji.sdk.keyvalue.value.gimbal.GimbalResetType
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GimbalVM : ViewModel() {

    private val TAG = "GimbalVM"

    private val _gimbalInfo = MutableStateFlow("no gimbal connection")
    val gimbalInfo: StateFlow<String> = _gimbalInfo

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared() called")
        resetGimbal()
    }

    init {
        describeGimbalInfo()
    }

    private fun describeGimbalInfo() {
        Log.d(TAG, "gimbal 연결됨 : ${KeyConnection.create().get()}")
        KeyGimbalAttitude.create().listen(this, false,  onChange = { newValue ->
            if(newValue != null){
                _gimbalInfo.value = String.format("%.2f", newValue.pitch)
            }
            else{
                _gimbalInfo.value = "no Info"
            }
        })
        return
    }

    fun setGimbalAngle() {
        KeyRotateByAngle.create().action(GimbalAngleRotation(
            GimbalAngleRotationMode.ABSOLUTE_ANGLE,
            -45.0,
            0.0,
            0.0,
            false,
            true,
            true,
            30.0,
            false,
            10
        ),
            onSuccess = { newValue ->
                Log.d(TAG, "gimbal 45도 적용 시작 : $newValue")
            }, onFailure = { newValue ->
                Log.d(TAG, "gimbal 45도 적용 실패 : $newValue")
            }
        )
    }

    fun resetGimbal() {
        KeyGimbalReset.create().action(GimbalResetType.PITCH_YAW, onSuccess = { _ ->
            Log.d(TAG, "gimbal 정면으로 초기화")

        }, onFailure = { newValue ->
            Log.d(TAG, "gimbal 정면으로 초기화 실패$newValue")
        })
    }
}
