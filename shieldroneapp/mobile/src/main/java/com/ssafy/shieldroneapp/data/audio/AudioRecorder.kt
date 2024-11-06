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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

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
    private val TAG = "모바일: 오디오 레코더"

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
                throw SecurityException("RECORD_AUDIO 권한 없음")
            }

            val buffer = ByteArray(bufferSize)
            Log.d(TAG, "$bufferSize bytes 버퍼 크기로 오디오 녹음 시작")

            while (isRecording) {
                val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                when {
                    readResult > 0 -> {
                        val maxAmplitude = buffer.take(readResult).maxOf { it.toInt() and 0xFF }
                        Log.d(TAG, "오디오 데이터 수집 완료 - 크기: $readResult 바이트, 최대 진폭: $maxAmplitude")

                        // 일정 주기로 샘플링 데이터 출력
                        if (System.currentTimeMillis() % 5000 < 100) { // 5초에 한 번 정도
                            val sample = buffer.take(min(50, readResult))
                                .joinToString(", ") { (it.toInt() and 0xFF).toString() }
                            Log.d(TAG, "오디오 샘플: [$sample]")
                        }

                        val audioData = AudioData(
                            time = System.currentTimeMillis(),
                            audioData = buffer.copyOf(readResult)
                        )
                        webSocketService.sendAudioData(audioData)
                    }
                    readResult == AudioRecord.ERROR_INVALID_OPERATION -> {
                        Log.e(TAG, "오디오 데이터 읽기 오류: 잘못된 형식")
                        break
                    }
                    readResult == AudioRecord.ERROR_BAD_VALUE -> {
                        Log.e(TAG, "오디오 데이터 읽기 오류: 잘못된 값")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "녹음 중 오류: ", e)
            handleRecordingError(e)
        }
    }

    private fun handleRecordingError(error: Exception) {
        when (error) {
            is SecurityException -> {
                Log.e(TAG, "권한 거부됨", error)
                stopRecording()
            }

            is IllegalStateException -> {
                Log.e(TAG, "오디오 기록 초기화 안됨", error)
                retryInitialization()
            }

            else -> {
                Log.e(TAG, "알 수 없는 에러 발생", error)
                stopRecording()
            }
        }
    }

    private fun retryInitialization() {
        recordingScope.launch {
            delay(1000)
            try {
                initializeAudioRecord()
                startRecording()
            } catch (e: Exception) {
                handleRecordingError(e)
            }
        }
    }

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