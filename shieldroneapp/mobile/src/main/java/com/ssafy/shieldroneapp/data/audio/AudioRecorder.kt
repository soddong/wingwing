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
import com.ssafy.shieldroneapp.data.source.remote.WebSocketState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
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
        private const val FRAMES_PER_BUFFER = 2560
        private const val BYTES_PER_FRAME = 2
        private const val TAG = "모바일: 오디오 레코더"
    }

    private val bufferSize = calculateBufferSize()
    private var isRecording = false

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
                    Log.d(TAG, "WebSocket 연결됨 - 녹음 시작 시도")
                    if (checkAudioPermission()) {
                        recordingScope.launch {
                            try {
                                if (!isRecording) {
                                    initializeAudioRecord()
                                    startRecordingInternal()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "WebSocket 연결 후 녹음 시작 실패", e)
                                handleRecordingError(e)
                            }
                        }
                    } else {
                        Log.e(TAG, "마이크 권한 없음 - 녹음 시작 불가")
                    }
                }
                is WebSocketState.Disconnected -> {
                    Log.d(TAG, "WebSocket 연결 해제됨 - 녹음 중지")
                    if (isRecording) {
                        stopRecording()
                    }
                }
                is WebSocketState.Error -> {
                    Log.e(TAG, "WebSocket 오류 발생 - 녹음 중지", state.throwable)
                    if (isRecording) {
                        stopRecording()
                    }
                }
            }
        }
    }

    // 녹음을 시작하는 함수
    fun startRecording() {
        Log.d(TAG, "startRecording 호출됨")
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

    // 오디오 데이터를 수집 및 전송
    private suspend fun collectAndSendAudioData() {
        try {
            val buffer = ByteArray(bufferSize)
            Log.d(TAG, "$bufferSize bytes 버퍼 크기로 오디오 녹음 시작")

            while (isRecording) {
                val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                when {
                    readResult > 0 -> {
                        // TODO: 여기서 음성 데이터 분석하여 dbFlag 결정
                        val dbFlag = analyzeAudioData(buffer, readResult)

                        val audioData = AudioData(
                            time = System.currentTimeMillis(),
                            dbFlag = dbFlag
                        )
                        Log.d(TAG, "오디오 데이터 분석 완료 - dbFlag: $dbFlag")
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

    private fun analyzeAudioData(buffer: ByteArray, size: Int): Boolean {
        // TODO: 여기서 실제 음성 분석
//        return false
    }

    private fun checkAudioPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "오디오 권한 상태: $hasPermission")
        return hasPermission
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