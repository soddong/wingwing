package com.ssafy.shieldroneapp.data.source.remote

/**
 * WebSocket을 통해 서버로부터 수신하는 알림 및 메시지를 구독하고 처리하는 클래스.
 *
 * 주로 위험 상황 알림을 단계별로 처리하며, 각 단계에 맞는 UI 동작을 수행
 *
 * 주요 메서드
 * - subscribeToDangerAlerts(): 위험 상황 알림을 구독하고 처리
 *
 * 알림 처리 방식:
 * - 1단계, 2단계: 알림을 표시하고 5초 후 자동으로 닫힘
 * - 3단계: 알림을 표시하며, 사용자가 '괜찮습니다. 위험하지 않습니다.'를 선택할 경우,
 *   서버에 응답을 전송하는 로직이 필요 (이 부분은 WebSocketMessageSender에서 처리)
 *
 * 이 클래스는 WebSocketService에 의해 import되어 사용됩니다.
 * 또한 WebSocketMessageParser를 import하여 수신한 메시지를 파싱합니다.
 */
import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.AlertData
import com.ssafy.shieldroneapp.data.repository.AlertRepository
import com.ssafy.shieldroneapp.services.alert.AlertService
import com.ssafy.shieldroneapp.ui.map.screens.AlertHandler
import com.ssafy.shieldroneapp.utils.await
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface SafetyMessageSender {
    suspend fun sendSafeConfirmationToWatch()
    suspend fun sendSafeConfirmationToMobile()
}

@Singleton
class WebSocketSubscriptions @Inject constructor(
    private val messageParser: WebSocketMessageParser,
    private val alertRepository: AlertRepository,
    private val alertHandler: AlertHandler,
    private val alertService: AlertService,
    @ApplicationContext private val context: Context,
) : SafetyMessageSender {
    companion object {
        private const val TAG = "모바일: 웹소켓 구독"
        private const val PATH_DANGER_ALERT = "/danger_alert"
        private const val PATH_OBJECT_ALERT = "/object_alert"
        private const val PATH_SAFE_CONFIRMATION = "/safe_confirm"
        private const val PATH_WATCH_SAFE_CONFIRMATION = "/safe_confirmation"
    }

    private val messageClient = Wearable.getMessageClient(context)
    private val subscriptionScope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()


    fun setupWatchMessageListener() {
        Wearable.getMessageClient(context).addListener { messageEvent ->
            when (messageEvent.path) {
                PATH_SAFE_CONFIRMATION -> {
                    val message = String(messageEvent.data)
                    try {
                        val confirmationData = gson.fromJson(message, Map::class.java)
                        val type = confirmationData["type"] as? String

                        if (type == "WATCH_ACKNOWLEDGED_SAFE") {
                            alertHandler.handleWatchSafeConfirmation() 
                            alertService.showSafeConfirmationNotification(
                                "안전 확인",
                                "워치에서 '안전' 확인이 되어 알림이 중지됩니다."
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "워치 안전 확인 메시지 처리 실패", e)
                    }
                }
            }
        }
    }

    fun handleIncomingMessage(message: String) {
        subscriptionScope.launch {
            try {
                val messageType = messageParser.getMessageType(message)
                Log.d(TAG, "수신된 메시지 타입: $messageType")

                when (messageType) {
                    "sendWarningFlag" -> {
                        Log.d(TAG, "위험 감지 메시지 수신")
                        val warningData = messageParser.parseWarningMessage(message)
                        if (warningData != null) {
                            Log.d(TAG, "위험 감지 메시지 파싱 성공 - warningFlag: ${warningData.warningFlag}")
                            val isSafeConfirmed =
                                alertHandler.getSafeConfirmationStatus()
                            handleWarningAlert(warningData.warningFlag, isSafeConfirmed)
                        } else {
                            Log.e(TAG, "위험 감지 메시지 파싱 실패")
                        }
                    }

                    "sendObjectFlag" -> {
                        Log.d(TAG, "타인 감지 메시지 수신")
                        val objectData = messageParser.parseObjectMessage(message)
                        if (objectData != null) {
                            Log.d(TAG, "타인 감지 메시지 파싱 성공 - objectFlag: ${objectData.objectFlag}")
                            handleObjectAlert(objectData.objectFlag)
                        } else {
                            Log.e(TAG, "타인 감지 메시지 파싱 실패")
                        }
                    }

                    PATH_SAFE_CONFIRMATION -> {
                        val confirmationData = Gson().fromJson(message, Map::class.java)
                        if (confirmationData["type"] == "WATCH_ACKNOWLEDGED_SAFE") {
                            handleDetailedWatchSafeConfirmation(confirmationData)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "메시지 처리 중 오류 발생", e)
            }
        }
    }

    private suspend fun handleWarningAlert(
        warningFlag: Boolean,
        userConfirmedSafe: Boolean = false,
    ) {
        if (warningFlag) {
            Log.d(TAG, "위험 감지 알림 시작")

            // UI 업데이트
            alertHandler.handleWarningBeep(warningFlag)
            Log.d(TAG, "위험 감지 UI 업데이트 완료")

            // 데이터 상태 관리
            alertRepository.updateWarningAlert()
            Log.d(TAG, "위험 감지 상태 업데이트 완료")

            // 시스템 알림 처리 (소리, 진동 등)
            alertService.handleWarningBeep(warningFlag)
            Log.d(TAG, "위험 감지 알림음 재생 완료")

            // 워치로 알림 전송
            sendDangerAlertToWatch(warningFlag)
            Log.d(TAG, "워치로 위험 감지 알림 전송 완료")

            // 사용자가 안전상황 버튼을 누른 경우
            if (userConfirmedSafe) {
                alertHandler.dismissAlert()
                alertService.showSafeConfirmationNotification(
                    "안전 확인",
                    "모바일 앱에서 '안전' 확인이 되었습니다."
                )
                sendSafeConfirmationToWatch()
            }
        } else {
            Log.d(TAG, "위험 감지 알림 초기화 시작")
            alertHandler.dismissAlert()
            Log.d(TAG, "위험 감지 알림 초기화 완료")
        }
    }

    private suspend fun handleObjectAlert(objectFlag: Boolean) {
        if (objectFlag) {

            // UI 업데이트
            Log.d(TAG, "물체 감지 알림 시작")
            alertHandler.handleObjectBeep(objectFlag)
            Log.d(TAG, "물체 감지 UI 업데이트 완료")

            // 데이터 상태 관리
            alertRepository.updateObjectAlert()
            Log.d(TAG, "위험 감지 상태 업데이트 완료")

            // 시스템 알림 처리 (소리, 진동 등)
            alertService.handleObjectBeep(objectFlag)
            Log.d(TAG, "물체 감지 알림음 재생 완료")

            // 워치로 알림 전송
            sendObjectAlertToWatch(objectFlag)
            Log.d(TAG, "워치로 물체 감지 알림 전송 완료")

            Log.d(TAG, "3초 후 물체 감지 알림 자동 종료 대기 중...")
            delay(3000)
            alertHandler.dismissObjectAlert()
            Log.d(TAG, "물체 감지 알림 자동 종료 완료")
        } else {
            Log.d(TAG, "물체 감지 알림 초기화 시작")
            alertHandler.dismissObjectAlert()
            Log.d(TAG, "물체 감지 알림 초기화 완료")
        }
    }

    private suspend fun sendDangerAlertToWatch(warningFlag: Boolean) {
        try {
            Log.d(TAG, "워치로 위험 감지 알림 전송 시작")
            val nodes = Wearable.getNodeClient(context).connectedNodes.await(5000)
            if (nodes.isEmpty()) {
                Log.e(TAG, "연결된 워치가 없어 위험 감지 알림 전송 불가")
                return
            }

            val alertData = AlertData(
                warningFlag = warningFlag,
                timestamp = System.currentTimeMillis()
            )

            val alertJson = gson.toJson(alertData)
            Log.d(TAG, "위험 감지 알림 데이터 생성 완료: $alertJson")

            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        PATH_DANGER_ALERT,
                        alertJson.toByteArray()
                    ).await(5000)
                    Log.d(
                        TAG,
                        "워치(${node.displayName})로 위험 감지 알림 전송 성공 - warningFlag: $warningFlag, timestamp: ${alertData.timestamp}"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "워치(${node.displayName})로 위험 감지 알림 전송 실패", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "워치 위험 감지 알림 전송 중 오류 발생", e)
            Log.e(TAG, "에러 메시지: ${e.message}")
            Log.e(TAG, "에러 원인: ${e.cause}")
            e.printStackTrace()
        }
    }

    private suspend fun sendObjectAlertToWatch(objectFlag: Boolean) {
        try {
            Log.d(TAG, "워치로 타인 감지 알림 전송 시작")
            val nodes = Wearable.getNodeClient(context).connectedNodes.await(5000)
            if (nodes.isEmpty()) {
                Log.e(TAG, "연결된 워치가 없어 타인 감지 알림 전송 불가")
                return
            }

            val alertData = AlertData(
                objectFlag = objectFlag,
                timestamp = System.currentTimeMillis()
            )

            val alertJson = gson.toJson(alertData)
            Log.d(TAG, "타인 감지 알림 데이터 생성 완료: $alertJson")

            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        PATH_OBJECT_ALERT,
                        alertJson.toByteArray()
                    ).await(5000)
                    Log.d(
                        TAG,
                        "워치(${node.displayName})로 타인 감지 알림 전송 성공 - objectFlag: $objectFlag, timestamp: ${alertData.timestamp}"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "워치(${node.displayName})로 타인 감지 알림 전송 실패", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "워치 타인 감지 알림 전송 중 오류 발생", e)
            Log.e(TAG, "에러 메시지: ${e.message}")
            Log.e(TAG, "에러 원인: ${e.cause}")
            e.printStackTrace()
        }
    }

    override suspend fun sendSafeConfirmationToWatch() {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await(5000)
            if (nodes.isNotEmpty()) {
                val confirmationData = gson.toJson(
                    mapOf(
                        "type" to "MOBILE_ACKNOWLEDGED_SAFE",
                        "state" to "CONFIRMED_SAFE",
                        "timestamp" to System.currentTimeMillis(),
                        "shouldCancelTimer" to true,
                        "message" to "모바일 앱에서 안전이 확인되었습니다"
                    )
                )

                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            PATH_SAFE_CONFIRMATION,
                            confirmationData.toByteArray()
                        ).await(5000)
                    } catch (e: Exception) {
                        Log.e(TAG, "워치(${node.displayName})로 안전 확인 메시지 전송 실패", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "안전 확인 메시지 전송 중 오류 발생", e)
        }
    }

    // 안전 확인 메시지를 워치에서 모바일로 전송
    override suspend fun sendSafeConfirmationToMobile() {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await(5000)
            if (nodes.isNotEmpty()) {
                val confirmationData = gson.toJson(
                    mapOf(
                        "type" to "WATCH_ACKNOWLEDGED_SAFE",
                        "state" to "CONFIRMED_SAFE",
                        "timestamp" to System.currentTimeMillis(),
                        "shouldCancelTimer" to true,
                        "message" to "워치에서 안전이 확인되었습니다"
                    )
                )

                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            PATH_SAFE_CONFIRMATION,
                            confirmationData.toByteArray()
                        ).await(5000)
                    } catch (e: Exception) {
                        Log.e(TAG, "모바일(${node.displayName})로 안전 확인 메시지 전송 실패", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "안전 확인 메시지 전송 중 오류 발생", e)
        }
    }

    // 단순 메시지
    private fun handleSimpleWatchSafeConfirmation() {
        try {
            alertHandler.setSafeConfirmation(true)

            alertService.showSafeConfirmationNotification(
                "안전 확인",
                "워치에서 '안전' 확인이 되었으므로 알림이 중지됩니다."
            )
        } catch (e: Exception) {
            Log.e(TAG, "워치 안전 확인 처리 중 오류 발생", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleDetailedWatchSafeConfirmation(confirmationData: Map<*, *>) {
        try {
            val typedData = confirmationData as Map<String, Any>

            alertHandler.dismissAlert()

            alertService.showSafeConfirmationNotification(
                "안전 확인",
                typedData["message"] as String? ?: "워치에서 '안전' 확인이 되었으므로 알림이 중지됩니다."
            )

            alertHandler.setSafeConfirmation(true)
        } catch (e: Exception) {
            Log.e(TAG, "워치 안전 확인 처리 중 오류 발생", e)
        }
    }
}