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
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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
        // ByteArray로 데시벨 및 주파수 계산
        val decibel = calculateDecibel(buffer)
        val frequency = calculateFrequency(buffer)

        Log.d(TAG, "데시벨: $decibel, 주파수: $frequency")

        // 비명 소리 감지: 특정 주파수 및 데시벨 임계값 확인
        if ((decibel > 60) && (frequency in 100.0..199.0)) {
            Log.e(TAG, "분석 결과, 남자 일반 목소리")
        } else if ((decibel > 60) && (frequency in 200.0..299.0)) {
            Log.e(TAG, "분석 결과, 여자 일반 목소리")
        } else if ((decibel > 60) && (frequency in 300.0..500.0)) {
            Log.e(TAG, "분석 결과, 남자 비명 목소리")
        } else if ((decibel > 60) && (frequency in 500.0..3000.0)) {
            Log.e(TAG, "분석 결과, 여자 비명 목소리")
        } else {
            Log.e(TAG, "분석 결과, 여자 일반 목소리")
        }
            return (decibel > 60) && (frequency in 200.0..3000.0)
    }

    private fun calculateDecibel(buffer: ByteArray): Double {
        var sum = 0.0
        for (i in buffer.indices step 2) {
            // 16-bit PCM 값으로 변환 (2 bytes per sample)
            val sample = ((buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)).toShort()
            sum += sample * sample
        }
        val rms = sqrt(sum / (buffer.size / 2))
        return 20 * log10(rms)
    }

    private fun calculateFrequency(buffer: ByteArray): Double {
        val fftBuffer = DoubleArray(buffer.size / 2)
        for (i in fftBuffer.indices) {
            // 16-bit PCM 값으로 변환
            fftBuffer[i] = ((buffer[2 * i].toInt() and 0xFF) or (buffer[2 * i + 1].toInt() shl 8)).toDouble()
        }

        val fftResult = fft(fftBuffer)

        var maxMagnitude = 0.0
        var maxIndex = 0
        for (i in fftResult.indices) {
            val magnitude = sqrt(fftResult[i].first * fftResult[i].first + fftResult[i].second * fftResult[i].second)
            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude
                maxIndex = i
            }
        }

        return maxIndex * SAMPLE_RATE / fftBuffer.size.toDouble()
    }

    private fun fft(buffer: DoubleArray): Array<Pair<Double, Double>> {
        val n = buffer.size
        if (n == 1) return arrayOf(Pair(buffer[0], 0.0))

        val even = DoubleArray(n / 2)
        val odd = DoubleArray(n / 2)
        for (i in 0 until n / 2) {
            even[i] = buffer[2 * i]
            odd[i] = buffer[2 * i + 1]
        }

        val evenFFT = fft(even)
        val oddFFT = fft(odd)

        val result = Array(n) { Pair(0.0, 0.0) }
        for (k in 0 until n / 2) {
            val angle = -2.0 * Math.PI * k / n
            val exp = Pair(Math.cos(angle), Math.sin(angle))
            val t = Pair(
                exp.first * oddFFT[k].first - exp.second * oddFFT[k].second,
                exp.first * oddFFT[k].second + exp.second * oddFFT[k].first
            )
            result[k] = Pair(evenFFT[k].first + t.first, evenFFT[k].second + t.second)
            result[k + n / 2] = Pair(evenFFT[k].first - t.first, evenFFT[k].second - t.second)
        }
        return result
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