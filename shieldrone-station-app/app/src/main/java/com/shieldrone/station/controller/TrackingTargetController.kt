package com.shieldrone.station.controller

import android.util.Log
import com.shieldrone.station.model.TrackingData
import com.shieldrone.station.model.TrackingTargetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket

class TrackingTargetController(private val viewModel: TrackingTargetViewModel) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var udpSocket: DatagramSocket? = null
    private var isReceiving = false

    companion object {
        private const val PORT = 11435
    }

    fun startReceivingData() {
        // 소켓이 이미 열려 있는지 확인하고 닫기
        Log.i("TrackingController", "Attempting to start receiving data...")
        stopReceivingData()  // 기존 소켓이 열려 있으면 닫음

        isReceiving = true
        Log.i("TrackingController", "Socket binding and receiving data started")
        coroutineScope.launch {
            receiveDataOverUDP()
        }
    }


    private suspend fun receiveDataOverUDP() {
        try {
            val buffer = ByteArray(1024)
            udpSocket = DatagramSocket(PORT)

            while (isReceiving) {
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)
                val message = String(packet.data, 0, packet.length)
                val data = JSONObject(message)
                val movement = when (val movementValue = data.get("movement")) {
                    is Int -> movementValue
                    is String -> {
                        // 문자열을 Int로 변환
                        when (movementValue) {
                            "forward" -> 1
                            "backward" -> -1
                            "stop" -> 0
                            else -> 0
                        }
                    }
                    else -> 0 // 기본값 (알 수 없는 타입인 경우)
                }

                val trackingData = TrackingData(
                    offsetX = data.getDouble("offset_x"),
                    offsetY = data.getDouble("offset_y"),
                    movement = movement,
                    boxWidth = data.getDouble("box_width"),
                    boxHeight = data.getDouble("box_height"),
                    normalizedOffsetX = data.getDouble("normalized_offset_x"),
                    normalizedOffsetY = data.getDouble("normalized_offset_y"),
                    isLocked = data.getBoolean("is_locked")
                )

                viewModel.updateTrackingData(trackingData)
            }
        } catch (e: Exception) {
            Log.e("TrackingController", "Error receiving data: ${e.message}")
        }
    }

    fun stopReceivingData() {
        isReceiving = false
        try {
            udpSocket?.let {
                if (!it.isClosed) {
                    it.close()
                    Log.i("TrackingController", "UDP Socket closed")
                }
            }
        } catch (e: Exception) {
            Log.e("TrackingController", "Error closing UDP socket: ${e.message}")
        } finally {
            udpSocket = null
        }
    }
}
