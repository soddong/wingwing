package com.ssafy.shieldroneapp.services.connection

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.ssafy.shieldroneapp.MainActivity
import com.ssafy.shieldroneapp.MainApplication
import com.ssafy.shieldroneapp.R
import com.ssafy.shieldroneapp.data.model.WatchConnectionState
import com.ssafy.shieldroneapp.data.repository.DataRepository
import com.ssafy.shieldroneapp.data.source.remote.AlertHandler
import com.ssafy.shieldroneapp.utils.await
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertHandler: AlertHandler,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private var dataRepository: DataRepository? = null
    private val _isMobileActive = mutableStateOf(false)
    val isMobileActive: State<Boolean> = _isMobileActive

    @Inject
    fun injectDataRepository(repository: DataRepository) {
        dataRepository = repository
    }

    private var monitoringCallback: MonitoringCallback? = null

    private val _isSafeConfirmed = MutableStateFlow(false)

    companion object {
        private const val TAG = "워치: 연결 매니저"
        private const val PATH_WATCH_STATUS = "/watch_status"
        private const val PATH_MOBILE_STATUS = "/mobile_status"
        private const val PATH_REQUEST_MOBILE_LAUNCH = "/request_mobile_launch"
        private const val PATH_DANGER_ALERT = "/danger_alert"
        private const val PATH_OBJECT_ALERT = "/object_alert"
        private const val PATH_SAFE_CONFIRMATION = "/safe_confirmation"
        private const val TYPE_MOBILE_SAFE = "MOBILE_SAFE"
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

    private val _connectionState =
        mutableStateOf<WatchConnectionState>(WatchConnectionState.Disconnected)

    private fun updateConnectionState(newState: WatchConnectionState) {
        scope.launch(Dispatchers.Main) {
            _connectionState.value = newState
        }
    }

    private fun setupMessageListener() {
        messageClient.addListener { messageEvent ->
            when (messageEvent.path) {
                PATH_WATCH_STATUS -> {
                    val isActive = messageEvent.data?.let { it[0] == 1.toByte() } ?: false
                    Log.d(TAG, "워치 상태 수신: ${if (isActive) "활성화" else "비활성화"}")
                    if (isActive) {
                        updateConnectionState(WatchConnectionState.Connected)
                    } else {
                        updateConnectionState(WatchConnectionState.Disconnected)
                    }
                }

                PATH_MOBILE_STATUS -> {
                    val isActive = messageEvent.data?.let { it[0] == 1.toByte() } ?: false
                    handleMobileStateChange(isActive)
                }

                PATH_DANGER_ALERT -> {
                    messageEvent.data?.let {
                        val alertJson = String(it)
                        Log.d(TAG, "위험 알림 수신: $alertJson")
                        alertHandler.handleDangerAlert(alertJson)
                    }
                }

                PATH_OBJECT_ALERT -> {
                    messageEvent.data?.let {
                        val alertJson = String(it)
                        Log.d(TAG, "물체 감지 알림 수신: $alertJson")
                        alertHandler.handleObjectAlert(alertJson)
                    }
                }

                PATH_SAFE_CONFIRMATION -> {
                    messageEvent.data?.let {
                        val confirmationJson = String(it)
                        try {
                            val confirmationData = Gson().fromJson(
                                confirmationJson,
                                Map::class.java
                            ) as Map<String, Any>
                            handleSafeConfirmation(confirmationData)
                        } catch (e: Exception) {
                            Log.e(TAG, "안전 확인 메시지 파싱 실패", e)
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkMobileConnection() {
        try {
            updateConnectionState(WatchConnectionState.Connecting())
            val nodes = getConnectedNodes()

            if (nodes.isEmpty()) {
                updateConnectionState(WatchConnectionState.Disconnected)
                requestMobileAppLaunchWithNotification()
                return
            }

            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        PATH_MOBILE_STATUS,
                        null
                    ).await(5000)
                } catch (e: Exception) {
                    updateConnectionState(WatchConnectionState.Error)
                    requestMobileAppLaunchWithNotification()
                }
            }
        } catch (e: Exception) {
            updateConnectionState(WatchConnectionState.Error)
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

    fun onAppStateChange(isActive: Boolean) {
        scope.launch {
            try {
                val nodes = getConnectedNodes()
                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            PATH_WATCH_STATUS,
                            if (isActive) byteArrayOf(1) else byteArrayOf(0)
                        ).await(5000)
                        Log.d(TAG, "워치 상태 전송: ${if (isActive) "활성화" else "비활성화"}")
                    } catch (e: Exception) {
                        Log.e(TAG, "워치 상태 전송 실패", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "워치 상태 변경 실패", e)
            }
        }
    }

    private fun handleSafeConfirmation(confirmationData: Map<String, Any>) {
        try {
            val messageType = confirmationData["type"] as? String
            val shouldCancelTimer = confirmationData["shouldDismiss"] as? Boolean ?: false

            if (messageType == TYPE_MOBILE_SAFE && shouldCancelTimer) {
                alertHandler.updateSafeConfirmation(true)
                _isSafeConfirmed.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "안전 확인 처리 중 오류 발생", e)
        }
    }

    private suspend fun getConnectedNodes(): List<Node> = withContext(Dispatchers.IO) {
        Wearable.getNodeClient(context).connectedNodes.await(5000)
    }

    suspend fun sendSafeConfirmationToMobile() {
        try {
            val nodes = getConnectedNodes()
            if (nodes.isNotEmpty()) {
                val confirmationData = mapOf(
                    "type" to "WATCH_ACKNOWLEDGED_SAFE",
                    "state" to "SAFE_CONFIRMED",
                    "timestamp" to System.currentTimeMillis(),
                    "shouldCancelTimer" to true
                )

                val confirmationJson = Gson().toJson(confirmationData)
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        PATH_SAFE_CONFIRMATION,
                        confirmationJson.toByteArray()
                    ).await(5000)
                }
                Log.d(TAG, "안전 확인 메시지 모바일로 전송 성공")
            }
        } catch (e: Exception) {
            Log.e(TAG, "안전 확인 메시지 전송 실패", e)
        }
    }
}
