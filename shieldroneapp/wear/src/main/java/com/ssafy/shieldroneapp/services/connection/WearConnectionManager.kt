package com.ssafy.shieldroneapp.services.connection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
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
    private val messageClient: MessageClient = Wearable.getMessageClient(context)  // @Inject 제거
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
        private const val PATH_MOBILE_APP_LAUNCH = "/request_mobile_launch"
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
        checkMobileConnection()
    }

    private fun setupMessageListener() {
        messageClient.addListener { messageEvent ->
            Log.d(TAG, "메시지 수신: ${messageEvent.path}") 
            when (messageEvent.path) {
                PATH_MOBILE_STATUS -> {
                    val isActive = messageEvent.data[0] == 1.toByte()
                    Log.d(TAG, "모바일 상태 변경: $isActive") 
                    handleMobileStateChange(isActive)
                }
            }
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
            }
        }
    }

    private fun checkMobileConnection() {
        scope.launch {
            try {
                val nodes = getConnectedNodes()
                Log.d(TAG, "연결된 노드 수: ${nodes.size}") 
                if (nodes.isEmpty()) {
                    handleMobileStateChange(false)
                    requestMobileAppLaunch()
                } else {
                    Log.d(TAG, "노드 발견, 모바일 앱 활성화 상태로 변경") 
                    handleMobileStateChange(true)
                    requestMobileAppState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check mobile connection", e)
                handleMobileStateChange(false)
            }
        }
    }


    private fun showMobileAppRequiredNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "mobile_required"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "모바일 앱 상태",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "모바일 앱 상태 관련 알림"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("모바일 앱 실행 필요")
            .setContentText("위험 감지를 위해 모바일 앱을 실행해주세요.")
            .setSmallIcon(R.drawable.shieldrone_ic)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private suspend fun requestMobileAppLaunch() {
        try {
            val nodes = getConnectedNodes()
            nodes.forEach { node ->
                messageClient.sendMessage(
                    node.id,
                    PATH_MOBILE_APP_LAUNCH,
                    null
                ).await(5000)
                Log.d(TAG, "Sent mobile app launch request to: ${node.displayName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request mobile app launch", e)
        }
    }

    private suspend fun requestMobileAppState() {
        try {
            val nodes = getConnectedNodes()
            nodes.forEach { node ->
                messageClient.sendMessage(
                    node.id,
                    PATH_MOBILE_STATUS,
                    null
                ).await(5000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request mobile app state", e)
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
                    Log.d(TAG, "워치 상태 전송 시도: ${node.displayName}")  // 추가
                    messageClient.sendMessage(
                        node.id,
                        PATH_WATCH_STATUS,
                        if (isActive) byteArrayOf(1) else byteArrayOf(0)
                    ).await(5000)
                }
                Log.d(TAG, "Watch state sent: $isActive")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send watch state", e)
            }
        }
    }

    fun isMobileConnected() = isMobileActive
}