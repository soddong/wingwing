package com.ssafy.shieldroneapp.data.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.ssafy.shieldroneapp.data.model.AudioData
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    private val context: Context,
    private val webSocketService: WebSocketService
) {
    private var audioRecord: AudioRecord? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
    }

    private val bufferSize = (AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT
    ) * BUFFER_SIZE_FACTOR)

    private var isRecording = false

    // 권한 체크 함수 추가
    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 녹음을 시작하는 함수
    fun startRecording() {
        if (isRecording) return

        // 권한 체크 추가
        if (!checkAudioPermission()) {
            throw SecurityException("RECORD_AUDIO permission not granted")
        }

        try {
            initializeAudioRecord()
            isRecording = true
            audioRecord?.startRecording()

            recordingScope.launch {
                collectAndSendAudioData()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting recording", e)
            throw e
        }
    }

    // AudioRecord 초기화
    private fun initializeAudioRecord() {
        try {
            if (checkAudioPermission()) {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                ).apply {
                    if (state != AudioRecord.STATE_INITIALIZED) {
                        throw IllegalStateException("AudioRecord initialization failed")
                    }
                }
            } else {
                throw SecurityException("RECORD_AUDIO permission not granted")
            }
        } catch (e: SecurityException) {
            Log.e("AudioRecorder", "Permission denied: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error initializing AudioRecord: ${e.message}")
            throw e
        }
    }

    // 오디오 데이터를 수집하고 전송하는 함수
    private suspend fun collectAndSendAudioData() {
        try {
            if (!checkAudioPermission()) {
                throw SecurityException("RECORD_AUDIO permission not granted")
            }

            val buffer = ByteArray(bufferSize)

            while (isRecording) {
                val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                when {
                    readResult > 0 -> {
                        try {
                            val audioData = AudioData(
                                time = System.currentTimeMillis(),
                                dbFlag = true
                            )
                            webSocketService.sendAudioData(audioData)
                        } catch (e: Exception) {
                            Log.e("AudioRecorder", "Error sending audio data", e)
                        }
                    }
                    readResult == AudioRecord.ERROR_INVALID_OPERATION -> {
                        Log.e("AudioRecorder", "Error reading audio data: invalid operation")
                        break
                    }
                    readResult == AudioRecord.ERROR_BAD_VALUE -> {
                        Log.e("AudioRecorder", "Error reading audio data: bad value")
                        break
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("AudioRecorder", "Permission denied while recording: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error during recording: ${e.message}")
            throw e
        } finally {
            stopRecording()
        }
    }

    // 녹음을 중지하는 함수
    fun stopRecording() {
        isRecording = false
        recordingScope.cancel()
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }
}