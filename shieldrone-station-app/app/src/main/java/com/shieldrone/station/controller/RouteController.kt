package com.shieldrone.station.controller

import android.util.Log
import com.shieldrone.station.service.route.RouteAdapter
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket

class RouteController (private val routeAdapter: RouteAdapter) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var udpSocket: DatagramSocket? = null
    private var isReceiving = false

    companion object {
        private const val HOST = "0.0.0.0"  // 모든 IP 주소에서 수신
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

                val latitude = data.getDouble("lat")
                val longitude = data.getDouble("lng")


                routeAdapter.process(latitude, longitude)
                // 수신한 데이터 출력
                Log.i("RouteController", "Received Location: lat=$latitude, lng=$longitude")
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
