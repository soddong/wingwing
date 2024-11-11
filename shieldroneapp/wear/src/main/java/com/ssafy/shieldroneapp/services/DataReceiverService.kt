package com.ssafy.shieldroneapp.services

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class DataReceiverService : WearableListenerService() {
    private var isHeartRateVibrating = false
    private var isDangerVibrating = false

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                when (event.dataItem.uri.path) {
                    "/sendPulseFlag" -> {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val pulseFlag = dataMap.getBoolean("pulseFlag")
                        if (pulseFlag && !isHeartRateVibrating) {
                            startHeartRateVibration()
                            isHeartRateVibrating = true
                        } else if (!pulseFlag && isHeartRateVibrating) {
                            stopHeartRateVibration()
                            isHeartRateVibrating = false
                        }
                    }
                    "/dangerAlert" -> {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val dangerFlag = dataMap.getBoolean("dangerFlag")
                        if (dangerFlag && !isDangerVibrating) {
                            startDangerVibration()
                            isDangerVibrating = true
                        } else if (!dangerFlag && isDangerVibrating) {
                            stopDangerVibration()
                            isDangerVibrating = false
                        }
                    }
                }
            }
        }
    }

    private fun startHeartRateVibration() {
        val vibrator = getVibrator()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun startDangerVibration() {
        val vibrator = getVibrator()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1초 진동, 0.5초 멈춤, 1초 진동 패턴
            vibrator.vibrate(VibrationEffect.createWaveform(
                longArrayOf(0, 1000, 500, 1000),  // 대기시간, 진동시간, 대기시간, 진동시간
                intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                -1  // -1은 반복하지 않음을 의미
            ))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 1000, 500, 1000), -1)
        }
    }

    private fun stopHeartRateVibration() {
        getVibrator().cancel()
    }

    private fun stopDangerVibration() {
        getVibrator().cancel()
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}