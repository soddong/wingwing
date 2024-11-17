package com.shieldrone.station.controller

import android.util.Log
import com.shieldrone.station.service.route.RouteAdapter
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket

class RouteController(private val routeAdapter: RouteAdapter) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var udpSocket: DatagramSocket? = null
    private var isReceiving = false

    companion object {
        private const val PORT = 23456      // RouteDecision에서 보낸 포트와 일치
    }

    init {
        try {
            udpSocket = DatagramSocket(PORT)
            Log.i("RouteController", "UDP Socket created and bound to port $PORT")
        } catch (e: Exception) {
            Log.e("RouteController", "Error creating UDP socket: ${e.message}")
        }
    }

    fun startReceivingLocation() {
        isReceiving = true
        coroutineScope.launch {
            receiveLocationOverUDP()
        }
    }

    private suspend fun receiveLocationOverUDP() {
        try {
            val buffer = ByteArray(1024)  // 수신할 최대 바이트 크기 설정

            while (isReceiving) {
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)  // 데이터 패킷 수신 대기

                val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val data = JSONObject(message)  // JSON 데이터로 변환

                // location 추출
                val location = data.getJSONObject("location")
                val locationLat = location.optDouble("lat", Double.NaN)
                val locationLng = location.optDouble("lng", Double.NaN)

                // dest_location 추출
                val destLocation = data.getJSONObject("dest_location")
                val destLat = destLocation.optDouble("lat", Double.NaN)
                val destLng = destLocation.optDouble("lng", Double.NaN)

                val startFlag = data.optBoolean("start_flag",false)
                // 데이터 처리
                routeAdapter.process(locationLat, locationLng, destLat, destLng,startFlag)

                // 수신한 데이터 출력
                Log.i(
                    "RouteController",
                    "Received Locations: location(lat=$locationLat, lng=$locationLng), " +
                            "dest_location(lat=$destLat, lng=$destLng), start_flag=$startFlag"
                )
            }

        } catch (e: Exception) {
            Log.e("RouteController", "Error receiving UDP packet: ${e.message}")
        }
    }

    fun stopReceivingLocation() {
        isReceiving = false
        closeSocket()
    }

    private fun closeSocket() {
        try {
            udpSocket?.close()
            Log.i("RouteController", "UDP Socket closed")
        } catch (e: Exception) {
            Log.e("RouteController", "Error closing UDP socket: ${e.message}")
        } finally {
            udpSocket = null
        }
    }
}
