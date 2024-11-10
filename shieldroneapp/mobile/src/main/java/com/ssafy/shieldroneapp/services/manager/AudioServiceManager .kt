package com.ssafy.shieldroneapp.services.manager

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ssafy.shieldroneapp.services.sensor.AudioRecordService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioServiceManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "모바일: 오디오 서비스 매니저"
    }

    private var isServiceRunning = false

    fun startAudioService() {
        if (!isServiceRunning) {
            Log.d(TAG, "오디오 서비스 시작")
            Intent(context, AudioRecordService::class.java).also { intent ->
                intent.action = AudioRecordService.ACTION_START_RECORDING
                context.startForegroundService(intent)
                isServiceRunning = true
            }
        }
    }

    fun stopAudioService() {
        if (isServiceRunning) {
            Log.d(TAG, "오디오 서비스 중지")
            Intent(context, AudioRecordService::class.java).also { intent ->
                intent.action = AudioRecordService.ACTION_STOP_RECORDING
                context.stopService(intent)
                isServiceRunning = false
            }
        }
    }

    fun isRunning() = isServiceRunning
}