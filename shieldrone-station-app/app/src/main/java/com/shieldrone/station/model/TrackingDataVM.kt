package com.shieldrone.station.model

import android.util.Log
import androidx.lifecycle.ViewModel
import com.shieldrone.station.constant.TrackingConstant
import com.shieldrone.station.data.TrackingData
import com.shieldrone.station.data.TrackingDataDiff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket

class TrackingDataVM : ViewModel() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var udpSocket: DatagramSocket? = null
    private val _trackingDataDiffFlow = MutableStateFlow<TrackingDataDiff?>(null)
    val trackingDataDiffFlow: StateFlow<TrackingDataDiff?> get() = _trackingDataDiffFlow

    private var previousData: TrackingData? = null

    init {
        startReceivingData()
    }

    override fun onCleared() {
        super.onCleared()
        stopReceivingData()
    }

    fun updateTrackingData(newData: TrackingData, isLocked: Boolean) {
        if (previousData != null && !isLocked) {//중간에 목표락이 풀리고 다시 락온이 됐을 때.
            previousData = null
            return
        }
        val oldData = previousData
        previousData = newData

        if (oldData != null) {
            val dataDiff = TrackingDataDiff(oldData, newData)
            _trackingDataDiffFlow.value = dataDiff
        } else {
            _trackingDataDiffFlow.value = null
        }
    }


    fun startReceivingData() {
        // 소켓이 이미 열려 있는지 확인하고 닫기
        Log.i("TrackingController", "Attempting to start receiving data...")
//        stopReceivingData()  // 기존 소켓이 열려 있으면 닫음

        Log.i("TrackingController", "Socket binding and receiving data started")
        coroutineScope.launch {
            receiveDataOverUDP()
        }
    }


//    private suspend fun receiveDataOverUDP() {
//        try {
//            val buffer = ByteArray(1024)
//            udpSocket = DatagramSocket(TrackingContstant.PORT)
//
//            while (true) {
//                val packet = DatagramPacket(buffer, buffer.size)
//                udpSocket?.receive(packet)
//                val message = String(packet.data, 0, packet.length)
//                val data = JSONObject(message)
//
//                val trackingData = TrackingData(
//                    receivedTime = System.currentTimeMillis(),
//                    boxWidth = data.getDouble("box_width"),
//                    boxHeight = data.getDouble("box_height"),
//                    normalizedOffsetX = data.getDouble("normalized_offset_x"),
//                    normalizedOffsetY = data.getDouble("normalized_offset_y")
//                )
//
//                updateTrackingData(trackingData, data.getBoolean("is_locked"))
//            }
//        } catch (e: Exception) {
//            Log.e("TrackingController", "Error receiving data: ${e.message}")
//        }
//    }

private suspend fun receiveDataOverUDP() {
    Log.i("TrackingController", "receiveDataOverUDP() 호출됨") // 추가 로그
    withContext(Dispatchers.IO) {
        Log.i("TrackingController", "withContext 블록 시작됨") // 추가 로그

        var udpSocket: DatagramSocket? = null
        try {
            val bufferSize = 1024
            Log.d("TrackingController", "소켓 생성 시도 중...") // 추가 로그
            udpSocket = DatagramSocket(TrackingConstant.PORT)
            Log.d("TrackingController", "UDP 소켓이 포트 ${TrackingConstant.PORT}에 바인딩되었습니다.")

            while (true) {
                // 버퍼 초기화
                val buffer = ByteArray(bufferSize)
                val packet = DatagramPacket(buffer, buffer.size)

                // 데이터 수신
                udpSocket.receive(packet)
                val message = String(packet.data, 0, packet.length).trim()
                Log.d("TrackingController", "데이터 수신: $message")

                // JSON 파싱 및 데이터 처리
                val data = JSONObject(message)
                val trackingData = TrackingData(
                    receivedTime = System.currentTimeMillis(),
                    boxWidth = data.getDouble("box_width"),
                    boxHeight = data.getDouble("box_height"),
                    normalizedOffsetX = data.getDouble("normalized_offset_x"),
                    normalizedOffsetY = data.getDouble("normalized_offset_y")
                )

                updateTrackingData(trackingData, data.getBoolean("is_locked"))
            }
        } catch (e: java.net.SocketException) {
            Log.e("TrackingController", "소켓 오류: ${e.message}")
        } catch (e: org.json.JSONException) {
            Log.e("TrackingController", "JSON 파싱 오류: ${e.message}")
        } catch (e: Exception) {
            Log.e("TrackingController", "알 수 없는 오류: ${e.message}")
        } finally {
            udpSocket?.close()
            Log.d("TrackingController", "UDP 소켓이 닫혔습니다.")
        }
    }
}

    fun stopReceivingData() {
        Log.i("TrackingController", "stopReceivingData() 호출됨") // 추가 로그
        try {
            udpSocket?.let {
                if (!it.isClosed) {
                    it.close()
                    Log.i("TrackingController", "UDP Socket closed")
                }
            }
            if (coroutineScope.isActive) {
                Log.i("TrackingController", "tracking coroutineScope cancel")
                coroutineScope.cancel()
            }
        } catch (e: Exception) {
            Log.e("TrackingController", "Error closing UDP socket: ${e.message}")
        } finally {
            udpSocket = null
        }
    }

}
