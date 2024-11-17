package com.ssafy.shieldroneapp.services.connection

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.MobileMainApplication
import com.ssafy.shieldroneapp.R
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.repository.HeartRateDataRepository
import com.ssafy.shieldroneapp.services.base.BaseMobileService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.Manifest
import android.content.ComponentName
import com.google.android.gms.wearable.Node
import com.ssafy.shieldroneapp.data.model.WatchConnectionState
import com.ssafy.shieldroneapp.data.source.remote.WebSocketSubscriptions

@AndroidEntryPoint
class WearableDataListenerService : BaseMobileService() {
    companion object {
        private const val TAG = "모바일: 웨어러블 기기 리스너"
        private const val NOTIFICATION_ID = 1001
        private const val PATH_HEART_RATE = "/sendPulseFlag"
        private const val PATH_WATCH_LAUNCHED = "/watch_app_launched"
        private const val PATH_REQUEST_MOBILE_LAUNCH = "/request_mobile_launch"
    }

    @Inject
    lateinit var heartRateDataRepository: HeartRateDataRepository

    @Inject
    lateinit var connectionManager: MobileConnectionManager

    @Inject
    lateinit var webSocketSubscriptions: WebSocketSubscriptions

    private lateinit var dataClient: DataClient

    override fun onCreate() {
        super.onCreate()
        dataClient = Wearable.getDataClient(this)
        heartRateDataRepository.startSendingLocalHeartRateData()
        checkExistingData()
        checkConnectedNodes()
        webSocketSubscriptions.setupWatchMessageListener()
    }

    override fun onPeerConnected(node: Node) {
        super.onPeerConnected(node)
        Log.d(TAG, "워치 연결됨: ${node.displayName}")
        connectionManager.updateWatchConnectionState(true)
        showNotification(
            "워치 연결됨",
            "워치와 연결되어 심박수 모니터링을 시작합니다."
        )
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (!isDataListeningEnabled) {
            Log.d(TAG, "데이터 수신이 비활성화되어 있어 메시지 무시: ${messageEvent.path}")
            return
        }

        super.onMessageReceived(messageEvent)
        Log.d(TAG, "메시지 수신됨: ${messageEvent.path}")

        when (messageEvent.path) {
            PATH_REQUEST_MOBILE_LAUNCH -> {
                Log.d(TAG, "워치로부터 모바일 앱 실행 요청 수신")
                launchMobileApp()
            }

            PATH_WATCH_LAUNCHED -> {
                Log.d(TAG, "워치 앱 실행 확인됨")
                connectionManager.updateWatchConnectionState(true)
                sendBroadcast(Intent("WATCH_APP_LAUNCHED"))
                showNotification(
                    "워치 연결됨",
                    "워치와 연결되어 심박수 모니터링을 시작합니다."
                )
            }

            MobileConnectionManager.PATH_WATCH_STATUS -> {
                val isWatchActive = messageEvent.data[0] == 1.toByte()
                connectionManager.updateWatchConnectionState(isWatchActive)

                if (!isWatchActive) {
                    showNotification(
                        "워치 연결 해제",
                        "정확한 위험 감지를 위해 워치 앱을 실행해주세요."
                    )
                }
            }
        }
    }

    private fun launchMobileApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            intent?.let {
                startActivity(it)
                Log.d(TAG, "모바일 앱 실행 인텐트 전송됨")
            }
        } catch (e: Exception) {
            Log.e(TAG, "모바일 앱 실행 실패", e)
        }
    }

    override fun onPeerDisconnected(node: Node) {
        super.onPeerDisconnected(node)
        Log.d(TAG, "워치 연결 해제됨: ${node.displayName}")

        // 연결이 의도치 않게 끊어진 경우 에러로 처리
        if (connectionManager.watchConnectionState.value is WatchConnectionState.Connected) {
            connectionManager.updateWatchConnectionStateWithError("워치와의 연결이 예기치 않게 끊어졌습니다")
            showNotification(
                "워치 연결 오류",
                "워치와의 연결이 비정상적으로 종료되었습니다"
            )
        } else {
            connectionManager.updateWatchConnectionState(false)
            showNotification(
                "워치 연결 해제",
                "워치와의 연결이 끊어졌습니다"
            )
        }
    }

    private fun showNotification(title: String, content: String) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(this).notify(
                    NOTIFICATION_ID,
                    NotificationCompat.Builder(this, MobileMainApplication.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.shieldrone_ic)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "알림 표시 실패", e)
        }
    }

    private fun checkExistingData() {
        dataClient.dataItems
            .addOnSuccessListener { dataItems ->
                Log.d(TAG, "현재 데이터 아이템 수: ${dataItems.count}")
                dataItems.forEach { item ->
                    Log.d(TAG, "데이터 아이템 발견: ${item.uri}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "데이터 아이템 확인 실패", e)
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand 호출됨")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        if (!isDataListeningEnabled) {
            Log.d(TAG, "데이터 수신이 비활성화되어 있어 데이터 변경 무시")
            return
        }

        Log.d(TAG, "onDataChanged 호출됨, 이벤트 수: ${dataEvents.count}")

        try {
            dataEvents.forEach { event ->
                Log.d(TAG, "이벤트 타입: ${event.type}, 경로: ${event.dataItem.uri.path}")
                Log.d(TAG, "전체 URI: ${event.dataItem.uri}")

                if (event.type == DataEvent.TYPE_CHANGED) {
                    when (event.dataItem.uri.path) {
                        PATH_HEART_RATE -> {
                            Log.d(TAG, "심박수 데이터 이벤트 감지됨")
                            processHeartRateData(event)
                        }

                        else -> Log.d(TAG, "알 수 없는 경로: ${event.dataItem.uri.path}")
                    }
                } else {
                    Log.d(TAG, "무시된 이벤트 타입: ${event.type}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "데이터 이벤트 처리 중 오류", e)
            e.printStackTrace()
        }
    }

    private fun checkConnectedNodes() {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                Log.d(TAG, "연결된 노드 수: ${nodes.size}")
                nodes.forEach { node ->
                    Log.d(TAG, "연결된 노드: ${node.displayName}, ID: ${node.id}")
                    requestDataFromNode(node.id)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "노드 확인 실패", e)
            }
    }

    private fun requestDataFromNode(nodeId: String) {
        Wearable.getMessageClient(this).sendMessage(
            nodeId,
            "/request/heart_rate_data",
            ByteArray(0)
        )
            .addOnSuccessListener {
                Log.d(TAG, "데이터 요청 메시지 전송 성공: $nodeId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "데이터 요청 메시지 전송 실패: $nodeId", e)
            }
    }

    private fun processHeartRateData(event: DataEvent) {
        try {
            val dataItem = event.dataItem
            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap

            val bpm = dataMap.getDouble("bpm", 0.0)
            val timestamp = dataMap.getLong("timestamp", System.currentTimeMillis())
            val availability = dataMap.getString("availability", "UNKNOWN")

            if (bpm > 0) {
                val heartRateData = HeartRateData(
                    pulseFlag = dataMap.getBoolean("pulseFlag", false),
                    bpm = bpm,
                    timestamp = timestamp
                )

                serviceScope.launch {
                    heartRateDataRepository.processHeartRateData(heartRateData)
                    Log.d(TAG, "새로운 심박수 데이터 처리됨: $bpm BPM, timestamp: $timestamp")
                }
            } else {
                val pulseFlag = dataMap.getBoolean("pulseFlag", false)
                val sustained = dataMap.getBoolean("sustained", false)

                val heartRateData = HeartRateData(
                    pulseFlag = pulseFlag,
                    bpm = 0.0,
                    timestamp = timestamp,
                    sustained = sustained
                )

                serviceScope.launch {
                    heartRateDataRepository.processHeartRateData(heartRateData)
                    Log.d(TAG, "상태 변경 데이터 처리됨: 심박수 - pulseFlag=$pulseFlag, sustained=$sustained")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 처리중 에러 발생", e)
            e.printStackTrace()
        }
    }
}