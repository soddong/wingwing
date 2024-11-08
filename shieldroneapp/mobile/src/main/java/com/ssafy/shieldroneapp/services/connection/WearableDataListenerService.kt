package com.ssafy.shieldroneapp.services.connection

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.repository.HeartRateDataRepository
import com.ssafy.shieldroneapp.services.base.BaseMobileService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WearableDataListenerService : BaseMobileService() {
    companion object {
        private const val TAG = "모바일: 웨어러블 기기 리스너"
        private const val PATH_HEART_RATE = "/sendPulseFlag"
        private const val PATH_WATCH_LAUNCHED = "/watch_app_launched"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "메시지 수신됨: ${messageEvent.path}")

        when (messageEvent.path) {
            PATH_WATCH_LAUNCHED -> {
                Log.d(TAG, "워치 앱 실행 확인됨")
                sendBroadcast(Intent("WATCH_APP_LAUNCHED"))
            }
        }
    }

    @Inject
    lateinit var heartRateDataRepository: HeartRateDataRepository

    private lateinit var dataClient: DataClient

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WearableDataListenerService 생성됨")
        dataClient = Wearable.getDataClient(this)
        // 서비스 시작 시 로컬 데이터 전송 시작
        heartRateDataRepository.startSendingLocalHeartRateData()

        // 현재 저장된 데이터 확인
        checkExistingData()
        // 연결된 노드 확인
        checkConnectedNodes()
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
                    // 노드별 데이터 요청
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
            Log.d(TAG, "데이터 아이템 처리 시작 - URI: ${dataItem.uri}")

            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
            Log.d(TAG, "데이터맵 키셋: ${dataMap.keySet()}")

            val pulseFlag = dataMap.getBoolean("pulseFlag")
            val timestamp = dataMap.getLong("timestamp")
            val sustained = dataMap.getBoolean("sustained", false)

            Log.d(TAG, "심박수 데이터 파싱 완료 - pulseFlag: $pulseFlag, timestamp: $timestamp, sustained: $sustained")

            serviceScope.launch {
                heartRateDataRepository.processHeartRateData(
                    HeartRateData(
                        pulseFlag = pulseFlag,
                        timestamp = timestamp
                    )
                )
                heartRateDataRepository.startSendingLocalHeartRateData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 처리중 에러 발생", e)
            e.printStackTrace()
        }
    }
}