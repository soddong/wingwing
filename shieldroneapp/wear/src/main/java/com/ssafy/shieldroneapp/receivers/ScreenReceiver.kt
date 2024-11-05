package com.ssafy.shieldroneapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ssafy.shieldroneapp.services.HeartRateService

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("ScreenReceiver", "화면 꺼짐")
                val serviceIntent = Intent(context, HeartRateService::class.java)
                context?.startForegroundService(serviceIntent)
            }
            Intent.ACTION_SCREEN_ON -> {
                Log.d("ScreenReceiver", "화면 켜짐")
            }
        }
    }
}