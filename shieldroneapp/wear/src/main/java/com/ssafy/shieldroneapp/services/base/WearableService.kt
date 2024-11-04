package com.ssafy.shieldroneapp.services

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.ssafy.shieldroneapp.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ssafy.shieldroneapp.viewmodels.HeartRateViewModel

class WearableService : WearableListenerService() {
    companion object {
        const val PATH_EMERGENCY_ALERT = "/emergency_alert"
        private var sensorViewModel: HeartRateViewModel? = null
        private var appContext: Context? = null

        fun setHeartRateViewModel(viewModel: HeartRateViewModel?) {
            sensorViewModel = viewModel
            Log.d("WearableService", "ViewModel ${if (viewModel != null) "설정됨" else "해제됨"}")
        }

        fun getHeartRateViewModel(): HeartRateViewModel? {
            return sensorViewModel
        }

        fun getContext() = MainApplication.getContext()
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        Log.d("WearableService", "WearableService onCreate, context 설정됨")
    }

    override fun onDestroy() {
        super.onDestroy()
        appContext = null
        Log.d("WearableService", "WearableService onDestroy")
    }
}