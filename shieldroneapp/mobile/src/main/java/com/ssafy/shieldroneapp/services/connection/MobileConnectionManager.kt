package com.ssafy.shieldroneapp.services.connection

/**
 * 모바일과 워치 간의 연결 상태를 관리하고, 데이터 전송 및 수신을 제어하는 클래스.
 *
 * 모바일-워치 간 연결 상태를 모니터링하고, 필요 시 재연결 로직을 실행하여 안정성을 보장한다.
 * 양방향 데이터 전송 요청을 처리하며, 끊김이 발생할 경우 자동으로 재연결하여 지속적인 데이터 흐름을 유지한다.
 */

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.data.model.WatchConnectionState
import com.ssafy.shieldroneapp.utils.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MobileConnectionManager @Inject constructor(
    private val context: Context
) {
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)

    private val _watchConnectionState = MutableStateFlow<WatchConnectionState>(WatchConnectionState.Disconnected)
    val watchConnectionState: StateFlow<WatchConnectionState> = _watchConnectionState

    companion object {
        private const val TAG = "모바일: 연결 매니저"
        const val PATH_MOBILE_STATUS = "/mobile_status"
        const val PATH_WATCH_STATUS = "/watch_status"
    }

    fun updateWatchConnectionState(isConnected: Boolean) {
        _watchConnectionState.value = if (isConnected) {
            WatchConnectionState.Connected
        } else {
            WatchConnectionState.Disconnected
        }
    }

    fun updateWatchConnectionStateWithError(errorMessage: String) {
        _watchConnectionState.value = WatchConnectionState.Error(errorMessage)
    }

    suspend fun notifyWatchOfMobileStatus(isActive: Boolean) {
        try {
            val nodes = nodeClient.connectedNodes.await(5000)
            if (nodes.isEmpty()) {
                updateWatchConnectionState(false)
                return
            }

            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        PATH_MOBILE_STATUS,
                        if (isActive) byteArrayOf(1) else byteArrayOf(0)
                    ).await(5000)
                    Log.d(TAG, "모바일 상태 전송 완료: ${if (isActive) "활성화" else "비활성화"}")
                    updateWatchConnectionState(isActive)
                } catch (e: Exception) {
                    Log.e(TAG, "모바일 상태 전송 실패", e)
                    updateWatchConnectionStateWithError("워치와의 통신 실패: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "모바일 상태 전송 실패", e)
            updateWatchConnectionStateWithError("워치 연결 실패: ${e.localizedMessage}")
        }
    }
}