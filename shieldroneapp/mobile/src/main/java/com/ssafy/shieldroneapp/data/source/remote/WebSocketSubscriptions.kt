package com.ssafy.shieldroneapp.data.source.remote

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
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

        // Message Paths
        private const val PATH_DANGER_ALERT = "/danger_alert"
        private const val PATH_OBJECT_ALERT = "/object_alert"
        private const val PATH_SAFE_CONFIRMATION = "/safe_confirmation"

        // Message Types
        private const val TYPE_WARNING_FLAG = "sendWarningFlag"
        private const val TYPE_OBJECT_FLAG = "sendObjectFlag"
        private const val TYPE_WATCH_SAFE = "WATCH_ACKNOWLEDGED_SAFE"
    }

    private val messageClient = Wearable.getMessageClient(context)
    private val subscriptionScope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()


    fun setupWatchMessageListener() {
        Wearable.getMessageClient(context).addListener { event ->
            when (event.path) {
                PATH_SAFE_CONFIRMATION -> {
                    val message = String(event.data)
                    try {
                        val confirmationData = Gson().fromJson(message, JsonObject::class.java)
                        val type = confirmationData.get("type")?.asString
                        val shouldCancelTimer = confirmationData.get("shouldCancelTimer")?.asBoolean ?: false

                        if (type == TYPE_WATCH_SAFE && shouldCancelTimer) {
                            handleSafeConfirmation(confirmationData)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "워치 안전 확인 메시지 처리 실패", e)
                    }
                }
            }
        }
    }

    private fun handleSafeConfirmation(confirmationData: JsonObject) {
        try {
            val type = confirmationData.get("type")?.asString
            val shouldCancelTimer = confirmationData.get("shouldCancelTimer")?.asBoolean ?: false

            if (type == TYPE_WATCH_SAFE && shouldCancelTimer) {
                alertHandler.handleWatchSafeConfirmation()
                alertService.showSafeConfirmationNotification(
                    "안전 확인",
                    "워치로부터 안전을 확인되었습니다."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "워치 안전 확인 메시지 처리 중 오류 발생", e)
        }
    }

    private fun JsonObject.asMap(): Map<String, Any> {
        val map = HashMap<String, Any>()
        for (entry in this.entrySet()) {
            map[entry.key] = entry.value.toPrimitive()
        }
        return map
    }

    private fun JsonElement.toPrimitive(): Any {
        return when (this) {
            is JsonPrimitive -> {
                if (this.isBoolean) this.asBoolean
                else if (this.isNumber) this.asNumber
                else this.asString
            }
            is JsonObject -> this.asMap()
            is JsonArray -> this.map { it.toPrimitive() }
            else -> throw IllegalArgumentException("지원되지 않는 JSON 요소 타입: $this")
        }
    }

    fun handleIncomingMessage(message: String) {
        subscriptionScope.launch {
            try {
                val messageType = messageParser.getMessageType(message)
                Log.d(TAG, "수신된 메시지 타입: $messageType")

                when (messageType) {
                    TYPE_WARNING_FLAG -> {
                        Log.d(TAG, "위험 감지 메시지 수신")
                        val warningData = messageParser.parseWarningMessage(message)
                        if (warningData != null) {
                            Log.d(TAG, "위험 감지 메시지 파싱 성공 - warningFlag: ${warningData.warningFlag}")
                            val isSafeConfirmed =
                                alertHandler.getSafeConfirmationStatus()
                            handleWarningAlert(warningData, isSafeConfirmed)
                        } else {
                            Log.e(TAG, "위험 감지 메시지 파싱 실패")
                        }
                    }

                    TYPE_OBJECT_FLAG -> {
                        Log.d(TAG, "타인 감지 메시지 수신")
                        val objectData = messageParser.parseObjectMessage(message)
                        if (objectData != null) {
                            Log.d(TAG, "타인 감지 메시지 파싱 성공 - objectFlag: ${objectData.objectFlag}")
                            handleObjectAlert(objectData.objectFlag)
                        } else {
                            Log.e(TAG, "타인 감지 메시지 파싱 실패")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "메시지 처리 중 오류 발생", e)
            }
        }
    }

    private suspend fun handleWarningAlert(
        warningData: WarningData,
        userConfirmedSafe: Boolean = false,
    ) {
        if (warningData.warningFlag) {
            Log.d(TAG, "위험 감지 알림 시작 ${if (warningData.frame != null) "- 프레임 포함" else ""}")

            // UI 업데이트
            alertHandler.handleWarningBeep(warningData.warningFlag)
            Log.d(TAG, "위험 감지 UI 업데이트 완료")

            // 데이터 상태 관리
            alertRepository.updateWarningAlert(warningData.frame)
            Log.d(TAG, "위험 감지 상태 업데이트 완료, $warningData.frame")

            // 시스템 알림 처리 (소리, 진동 등)
            alertService.handleWarningBeep(warningData.warningFlag)
            Log.d(TAG, "위험 감지 알림음 재생 완료")

            // 워치로 알림 전송
            sendDangerAlertToWatch(warningData)
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

    private suspend fun sendDangerAlertToWatch(warningData: WarningData) {
        try {
            Log.d(TAG, "워치로 위험 감지 알림 전송 시작")
            val nodes = Wearable.getNodeClient(context).connectedNodes.await(5000)
            if (nodes.isEmpty()) {
                Log.e(TAG, "연결된 워치가 없어 위험 감지 알림 전송 불가")
                return
            }

            val alertData = AlertData(
                warningFlag = warningData.warningFlag,
                objectFlag = false,
                timestamp = System.currentTimeMillis(),
                frame = warningData.frame
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
                        "워치(${node.displayName})로 위험 감지 알림 전송 성공 - warningFlag: ${warningData.warningFlag}, timestamp: ${alertData.timestamp}"
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
                val messageData = mapOf(
                    "type" to "MOBILE_SAFE",
                    "state" to "SAFE_CONFIRMED",
                    "timestamp" to System.currentTimeMillis(),
                    "shouldDismiss" to true
                )

                val confirmationJson = gson.toJson(messageData)
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        PATH_SAFE_CONFIRMATION,
                        confirmationJson.toByteArray()
                    ).await(5000)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "안전 확인 메시지 전송 중 오류 발생", e)
        }
    }

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
}