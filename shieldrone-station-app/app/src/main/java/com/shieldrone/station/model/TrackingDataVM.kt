package com.shieldrone.station.model

import android.util.Log
import androidx.lifecycle.ViewModel
import com.shieldrone.station.data.TrackingDataDiff
import com.shieldrone.station.data.TrackingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket

class TrackingDataVM : ViewModel() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var udpSocket: DatagramSocket? = null
    private val _trackingDataDiffFlow = MutableStateFlow<TrackingDataDiff?>(null)
    val trackingDataDiffFlow: StateFlow<TrackingDataDiff?> get() = _trackingDataDiffFlow

    private var previousData: TrackingData? = null

    companion object {
        private const val PORT = 11435
    }

    fun updateTrackingData(newData: TrackingData, isLocked: Boolean) {
        if(previousData != null && !isLocked) {//중간에 목표락이 풀리고 다시 락온이 됐을 때.
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
        stopReceivingData()  // 기존 소켓이 열려 있으면 닫음

        Log.i("TrackingController", "Socket binding and receiving data started")
        coroutineScope.launch {
            receiveDataOverUDP()
        }
    }


    private suspend fun receiveDataOverUDP() {
        try {
            val buffer = ByteArray(1024)
            udpSocket = DatagramSocket(PORT)

            while (true) {
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)
                val message = String(packet.data, 0, packet.length)
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
        } catch (e: Exception) {
            Log.e("TrackingController", "Error receiving data: ${e.message}")
        }
    }

    fun stopReceivingData() {
        try {
            udpSocket?.let {
                if (!it.isClosed) {
                    it.close()
                    Log.i("TrackingController", "UDP Socket closed")
                }
            }
            if(coroutineScope.isActive) {
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
