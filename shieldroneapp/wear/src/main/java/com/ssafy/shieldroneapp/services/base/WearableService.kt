package com.ssafy.shieldroneapp.services

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.ssafy.shieldroneapp.app.MainApplication
import com.ssafy.shieldroneapp.core.utils.await
import com.ssafy.shieldroneapp.features.heartrate.HeartRateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WearableService : WearableListenerService() {
    companion object {
        const val PATH_EMERGENCY_ALERT = "/emergency_alert"
        const val PATH_START_HEART_RATE = "/start/heart_rate_monitor"
        const val PATH_START = "/start"
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
            PATH_START -> {
                Log.d(TAG, "앱 실행 요청 수신")
                handleStartMessage()
            }
            PATH_START_HEART_RATE -> {
                Log.d(TAG, "심박수 모니터링 시작 요청 수신")
                startHeartRateService()
            }
        }
    }

    private fun handleStartMessage() {
        try {
            // 앱이 이미 실행 중인지 확인
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningApps = activityManager.runningAppProcesses
            val isRunning = runningApps?.any {
                it.processName == packageName &&
                        it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            } ?: false

            if (!isRunning) {
                val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                intent?.let {
                    startActivity(it)
                    sendLaunchConfirmation()
                }
            } else {
                sendLaunchConfirmation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "앱 실행 처리 중 오류 발생", e)
        }
    }

    private fun sendLaunchConfirmation() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Wearable.getNodeClient(this@WearableService)
                    .connectedNodes.await(5000)

                nodes.forEach { node ->
                    Wearable.getMessageClient(this@WearableService)
                        .sendMessage(
                            node.id,
                            "/watch_app_launched",
                            null
                        ).await(5000)
                }
                Log.d(TAG, "실행 확인 메시지 전송 성공")
            } catch (e: Exception) {
                Log.e(TAG, "실행 확인 메시지 전송 실패", e)
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