package com.ssafy.shieldroneapp.data.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.ssafy.shieldroneapp.data.model.AudioData
import com.ssafy.shieldroneapp.data.repository.AudioDataRepository
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.data.source.remote.WebSocketState
import com.ssafy.shieldroneapp.services.alert.AlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Singleton
class AudioRecorder @Inject constructor(
    private val context: Context,
    private val audioDataRepository: AudioDataRepository,
    private val webSocketService: WebSocketService,
    private val audioAnalyzer: AudioAnalyzer,
    private val alertService: AlertService,
) {
    private var audioRecord: AudioRecord? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO + Job())
    private var isRecording = false

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAMES_PER_BUFFER = 2560
        private const val BYTES_PER_FRAME = 2
        private const val TAG = "모바일: 오디오 레코더"
    }

    private val bufferSize = calculateBufferSize()

    private fun calculateBufferSize(): Int {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        val frameSize = FRAMES_PER_BUFFER * BYTES_PER_FRAME
        return max(minBufferSize, frameSize)
    }

    init {
        webSocketService.setAudioRecorderCallback { state ->
            when (state) {
                is WebSocketState.Connected -> {
                    Log.d(TAG, "WebSocket 연결됨 - 녹음 상태 확인")
                    if (checkAudioPermission() && !isRecording) {
                        recordingScope.launch {
                            try {
                                startRecording()
                            } catch (e: Exception) {
                                Log.e(TAG, "WebSocket 연결 후 녹음 시작 실패", e)
                                handleRecordingError(e)
                            }
                        }
                    }
                }

                is WebSocketState.Disconnected -> {
                    Log.d(TAG, "WebSocket 연결 해제됨")
                    // 연결이 끊어져도 녹음은 계속 유지
                    // 데이터 로컬에 저장됨
                }

                is WebSocketState.Error -> {
                    Log.e(TAG, "WebSocket 오류 발생", state.throwable)
                    // 오류가 발생해도 녹음은 계속 유지
                    // 데이터 로컬에 저장됨
                }
            }
        }
    }

    fun startRecording() {
        if (!checkAudioPermission()) {
            Log.e(TAG, "RECORD_AUDIO 권한 없음")
            throw SecurityException("RECORD_AUDIO permission not granted")
        }

        recordingScope.launch {
            try {
                if (!isRecording) {
                    initializeAudioRecord()
                    startRecordingInternal()
                } else {
                    Log.d(TAG, "이미 녹음 중")
                }
            } catch (e: Exception) {
                Log.e(TAG, "녹음 시작 중 오류", e)
                handleRecordingError(e)
            }
        }
    }

    private fun startRecordingInternal() {
        Log.d(TAG, "녹음 초기화 시작")
        isRecording = true
        audioRecord?.startRecording()
        Log.d(TAG, "녹음 시작됨")

        recordingScope.launch {
            Log.d(TAG, "오디오 데이터 수집 시작")
            collectAndSendAudioData()
        }
    }

    private suspend fun collectAndSendAudioData() {
        try {
            val buffer = ByteArray(bufferSize)
            Log.d(TAG, "$bufferSize bytes 버퍼 크기로 오디오 녹음 시작")

            while (isRecording) {
                val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                when {
                    readResult > 0 -> {
                        val analysisResult = audioAnalyzer.analyzeAudioData(
                            buffer = buffer,
                            enableDetailedAnalysis = true 
                        )

                        if (analysisResult.shouldShowToast) {
                            alertService.showSafeConfirmationNotification(
                                "주변에 소음 발생!",
                                "안전에 유의하세요."
                            )
                        }

                        val audioData = AudioData(
                            time = System.currentTimeMillis(),
                            dbFlag = analysisResult.shouldSendToServer
                        )

                        try {
                            audioDataRepository.processAudioData(audioData)
                        } catch (e: Exception) {
                            Log.e(TAG, "오디오 데이터 처리 실패", e)
                        }
                    }

                    readResult == AudioRecord.ERROR_INVALID_OPERATION -> {
                        Log.e(TAG, "오디오 데이터 읽기 오류: 잘못된 형식")
                        handleRecordingError(IllegalStateException("Invalid operation"))
                        break
                    }

                    readResult == AudioRecord.ERROR_BAD_VALUE -> {
                        Log.e(TAG, "오디오 데이터 읽기 오류: 잘못된 값")
                        handleRecordingError(IllegalArgumentException("Bad value"))
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "녹음 중 오류: ", e)
            handleRecordingError(e)
        }
    }

    private fun checkAudioPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "오디오 권한 상태: $hasPermission")
        return hasPermission
    }

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
            Log.e(TAG, "Permission denied: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioRecord: ${e.message}")
            throw e
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
        audioDataRepository.stopSendingData()

        recordingScope.cancel()
        audioRecord?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e(TAG, "오디오 녹음 중지 중 오류 발생", e)
            }
        }
        audioRecord = null
    }
}