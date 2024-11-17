package com.ssafy.shieldroneapp.data.audio

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

@Singleton
class AudioAnalyzer @Inject constructor() {
    companion object {
        private const val TAG = "모바일: 오디오 분석기"
        private const val SAMPLE_RATE = 16000

        // 토스트 알림용 데시벨 범위
        private const val TOAST_DECIBEL_MIN = 60.0
        private const val TOAST_DECIBEL_MAX = 70.0

        // 서버 전송용 데시벨 임계값 (토스트 최대값 초과)
        private const val SERVER_DECIBEL_THRESHOLD = TOAST_DECIBEL_MAX

        // 주파수 범위
        private const val MALE_NORMAL_FREQ_MIN = 100.0
        private const val MALE_NORMAL_FREQ_MAX = 199.0
        private const val FEMALE_NORMAL_FREQ_MIN = 200.0
        private const val FEMALE_NORMAL_FREQ_MAX = 299.0
        private const val MALE_SCREAM_FREQ_MIN = 300.0
        private const val MALE_SCREAM_FREQ_MAX = 500.0
        private const val FEMALE_SCREAM_FREQ_MIN = 500.0
        private const val FEMALE_SCREAM_FREQ_MAX = 3000.0
    }

    data class AudioAnalysisResult(
        val shouldShowToast: Boolean,
        val shouldSendToServer: Boolean
    )

    fun analyzeAudioData(buffer: ByteArray, enableDetailedAnalysis: Boolean = false): AudioAnalysisResult {
        val decibel = calculateDecibel(buffer)
        val frequency = calculateFrequency(buffer)

        Log.d(TAG, "데시벨: $decibel, 주파수: $frequency, $enableDetailedAnalysis")

        // 소리 유형 분석
        if (enableDetailedAnalysis) {
            when {
                isInFrequencyRange(frequency, MALE_NORMAL_FREQ_MIN, MALE_NORMAL_FREQ_MAX) -> {
                    Log.d(TAG, "분석 결과, 남자 일반 목소리")
                }
                isInFrequencyRange(frequency, FEMALE_NORMAL_FREQ_MIN, FEMALE_NORMAL_FREQ_MAX) -> {
                    Log.d(TAG, "분석 결과, 여자 일반 목소리")
                }
                isInFrequencyRange(frequency, MALE_SCREAM_FREQ_MIN, MALE_SCREAM_FREQ_MAX) -> {
                    Log.d(TAG, "분석 결과, 남자 비명 목소리")
                }
                isInFrequencyRange(frequency, FEMALE_SCREAM_FREQ_MIN, FEMALE_SCREAM_FREQ_MAX) -> {
                    Log.d(TAG, "분석 결과, 여자 비명 목소리")
                }
                else -> {
                    Log.d(TAG, "분석 결과, 해당 없음")
                }
            }
        }

        val frequencyInRange = isInFrequencyRange(
            frequency,
            FEMALE_NORMAL_FREQ_MIN,
            FEMALE_SCREAM_FREQ_MAX
        )

        return AudioAnalysisResult(
            shouldShowToast = isInToastDecibelRange(decibel) && frequencyInRange,
            shouldSendToServer = isAboveServerThreshold(decibel) && frequencyInRange
        )
    }

    private fun calculateDecibel(buffer: ByteArray): Double {
        var sum = 0.0
        for (i in buffer.indices step 2) {
            val sample = ((buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)).toShort()
            sum += sample * sample
        }
        val rms = sqrt(sum / (buffer.size / 2))
        return 20 * log10(rms)
    }

    private fun calculateFrequency(buffer: ByteArray): Double {
        val fftBuffer = DoubleArray(buffer.size / 2)
        for (i in fftBuffer.indices) {
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

    private fun isInFrequencyRange(frequency: Double, min: Double, max: Double): Boolean {
        return frequency in min..max
    }

    private fun isInToastDecibelRange(decibel: Double): Boolean {
        return decibel in TOAST_DECIBEL_MIN..TOAST_DECIBEL_MAX
    }

    private fun isAboveServerThreshold(decibel: Double): Boolean {
        return decibel > SERVER_DECIBEL_THRESHOLD
    }
}