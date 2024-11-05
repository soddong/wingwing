package com.ssafy.shieldroneapp.services.sensor

import android.content.Intent
import com.ssafy.shieldroneapp.data.audio.AudioRecorder
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.services.base.BaseMobileService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class AudioRecordService : BaseMobileService() {
    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var webSocketService: WebSocketService

    private var isRecording = false

    override fun onCreate() {
        super.onCreate()
        webSocketService.initialize()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (!isRecording) {
            isRecording = true
            serviceScope.launch {
                audioRecorder.startRecording()
            }
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            isRecording = false
            audioRecorder.stopRecording()
            stopForeground(true)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
        const val NOTIFICATION_ID = 1
    }
}

