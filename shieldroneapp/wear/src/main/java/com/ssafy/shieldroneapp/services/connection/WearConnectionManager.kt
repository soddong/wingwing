package com.ssafy.shieldroneapp.services.connection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.MainActivity
import com.ssafy.shieldroneapp.MainApplication
import com.ssafy.shieldroneapp.R
import com.ssafy.shieldroneapp.data.repository.DataRepository
import com.ssafy.shieldroneapp.utils.await
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private var dataRepository: DataRepository? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _isMobileActive = mutableStateOf(false)
    val isMobileActive: State<Boolean> = _isMobileActive

    @Inject
    fun injectDataRepository(repository: DataRepository) {
        dataRepository = repository
    }

    private var monitoringCallback: MonitoringCallback? = null

    companion object {
        private const val TAG = "워치: 연결 매니저"
        private const val PATH_WATCH_STATUS = "/watch_status"
        private const val PATH_MOBILE_STATUS = "/mobile_status"
        private const val PATH_REQUEST_MOBILE_LAUNCH = "/request_mobile_launch"
    }

    interface MonitoringCallback {
        fun pauseMonitoring()
        fun resumeMonitoring()
    }

    fun setMonitoringCallback(callback: MonitoringCallback) {
        monitoringCallback = callback
    }

    fun initialize() {
        setupMessageListener()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            checkMobileConnection()
        }
    }

    private fun setupMessageListener() {
        messageClient.addListener { messageEvent ->
            Log.d(TAG, "메시지 수신: ${messageEvent.path}")
            when (messageEvent.path) {
                PATH_MOBILE_STATUS -> {
                    val isActive = messageEvent.data?.let { it[0] == 1.toByte() } ?: false
                    Log.d(TAG, "모바일 상태 변경: $isActive")
                    handleMobileStateChange(isActive)
                }
            }
        }
    }

    private suspend fun checkMobileConnection() {
        try {
            val nodes = getConnectedNodes()
            Log.d(TAG, "연결된 노드 수: ${nodes.size}")

            handleMobileStateChange(false) 

            if (nodes.isNotEmpty()) {
                // 모바일 앱 상태 요청
                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            PATH_MOBILE_STATUS,
                            null
                        ).await(5000)
                        Log.d(TAG, "모바일 앱 상태 요청 전송됨")
                    } catch (e: Exception) {
                        Log.e(TAG, "모바일 앱 상태 요청 실패", e)
                        requestMobileAppLaunchWithNotification()
                    }
                }
            } else {
                requestMobileAppLaunchWithNotification()
            }
        } catch (e: Exception) {
            Log.e(TAG, "모바일 앱 연결 확인 실패", e)
            handleMobileStateChange(false)
            requestMobileAppLaunchWithNotification()
        }
    }

    private fun startWatchApp() {
        if (isMobileActive.value) {
            val application = MainApplication.getContext() as MainApplication
            val intent = Intent(application, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(intent)
        }
    }

    private fun handleMobileStateChange(isActive: Boolean) {
        Log.d(TAG, "handleMobileStateChange 호출: $isActive")
        if (_isMobileActive.value != isActive) {
            _isMobileActive.value = isActive
            Log.d(TAG, "isMobileActive 값 변경: $isActive")
            if (!isActive) {
                showMobileAppRequiredNotification()
                dataRepository?.pauseMonitoring()
            } else {
                dataRepository?.resumeMonitoring()
                startWatchApp()
            }
        }
    }

    private fun showMobileAppRequiredNotification() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "mobile_app_channel")
            .setContentTitle("모바일 앱 실행 필요")
            .setContentText("정확한 위험 감지를 위해 모바일 앱을 실행해주세요")
            .setSmallIcon(R.drawable.shieldrone_ic)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1001, notification)
    }

    private suspend fun requestMobileAppLaunchWithNotification() {
        try {
            val nodes = getConnectedNodes()
            nodes.forEach { node ->
                messageClient.sendMessage(
                    node.id,
                    PATH_REQUEST_MOBILE_LAUNCH,
                    null
                ).await(5000)
                Log.d(TAG, "모바일 앱 실행 요청 전송: ${node.displayName}")
            }
            showMobileAppRequiredNotification()
        } catch (e: Exception) {
            Log.e(TAG, "모바일 앱 실행 요청 실패", e)
        }
    }

    fun requestMobileAppLaunch() {
        scope.launch {
            requestMobileAppLaunchWithNotification()
        }
    }

    private suspend fun getConnectedNodes(): List<Node> = withContext(Dispatchers.IO) {
        Wearable.getNodeClient(context).connectedNodes.await(5000)
    }

    fun onAppStateChange(isActive: Boolean) {
        scope.launch {
            try {
                val nodes = getConnectedNodes()
                nodes.forEach { node ->
                    Log.d(TAG, "워치 상태 전송 시도: ${node.displayName}")
                    messageClient.sendMessage(
                        node.id,
                        PATH_WATCH_STATUS,
                        if (isActive) byteArrayOf(1) else byteArrayOf(0)
                    ).await(5000)
                }
                Log.d(TAG, "워치 상태 전송: $isActive")
            } catch (e: Exception) {
                Log.e(TAG, "워치 상태 전송 실패", e)
            }
        }
    }

    fun isMobileConnected() = isMobileActive
}