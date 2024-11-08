package com.ssafy.shieldroneapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ssafy.shieldroneapp.services.HeartRateService

class ScreenReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "워치: 스크린 리시버"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "화면 꺼짐")
                context?.let {
                    val serviceIntent = Intent(it, HeartRateService::class.java)
                    it.startForegroundService(serviceIntent)
                }
            }
            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "화면 켜짐")
            }
        }
    }
}