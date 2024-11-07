package com.ssafy.shieldroneapp.services

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.ssafy.shieldroneapp.MainApplication
import com.ssafy.shieldroneapp.viewmodels.HeartRateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WearableService : WearableListenerService() {
    companion object {
        const val PATH_EMERGENCY_ALERT = "/emergency_alert"
        const val PATH_START_HEART_RATE = "/start/heart_rate_monitor"
        private var sensorViewModel: HeartRateViewModel? = null
        private var appContext: Context? = null
        private const val TAG = "워치: 웨어러블 서비스"

        fun setHeartRateViewModel(viewModel: HeartRateViewModel?) {
            sensorViewModel = viewModel
            Log.d(TAG, "ViewModel ${if (viewModel != null) "설정됨" else "해제됨"}")
        }

        fun getHeartRateViewModel(): HeartRateViewModel? {
            return sensorViewModel
        }

        fun getContext() = MainApplication.getContext()
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        Log.d(TAG, "WearableService onCreate, context 설정됨")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "메시지 수신됨: ${messageEvent.path}")

        when (messageEvent.path) {
            PATH_START_HEART_RATE -> {
                Log.d(TAG, "심박수 모니터링 시작 요청 수신")
                startHeartRateService()
            }
        }
    }

    private fun startHeartRateService() {
        try {
            val serviceIntent = Intent(this, HeartRateService::class.java)
            startService(serviceIntent)
            Log.d(TAG, "심박수 서비스 시작됨")
        } catch (e: Exception) {
            Log.e(TAG, "심박수 서비스 시작 실패", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appContext = null
        Log.d(TAG, "WearableService onDestroy")
    }
}