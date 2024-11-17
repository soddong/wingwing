package com.shieldrone.station.controller

import android.util.Log
import com.shieldrone.station.service.route.RouteAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
//        try {
        udpSocket = DatagramSocket(PORT)
        Log.i("RouteController", "UDP Socket created and bound to port $PORT")
//        } catch (e: Exception) {
//            Log.e("RouteController", "Error creating UDP socket: ${e.message}")
//        }
    }

    fun startReceivingLocation() {
        isReceiving = true
        coroutineScope.launch {
            receiveLocationOverUDP()
        }
    }

    private suspend fun receiveLocationOverUDP() {

        val buffer = ByteArray(1024)  // 수신할 최대 바이트 크기 설정

        while (isReceiving) {
            val packet = DatagramPacket(buffer, buffer.size)
            udpSocket?.receive(packet)  // 데이터 패킷 수신 대기

            val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
            val data = JSONObject(message)  // JSON 데이터로 변환

            val startFlag = data.optBoolean("start_flag", false)

            if (startFlag) {
                // start_flag가 true인 경우
                Log.i("RouteController", "Received start_flag: $startFlag (No location data expected)")
                // 필요한 추가 로직
            } else if (data.has("location") && data.has("dest_location")) {
                // location과 dest_location이 모두 존재하는 경우
                val location = data.getJSONObject("location")
                val destLocation = data.getJSONObject("dest_location")

                val locationLat = location.optDouble("lat", Double.NaN)
                val locationLng = location.optDouble("lng", Double.NaN)
                val destLat = destLocation.optDouble("lat", Double.NaN)
                val destLng = destLocation.optDouble("lng", Double.NaN)

                // 데이터 처리
                routeAdapter.process(locationLat, locationLng, destLat, destLng, startFlag)
                // 로그 출력
            } else {
                // 데이터가 누락된 경우 처리
                Log.e("RouteController", "Missing location or dest_location data.")
                // 필요에 따라 기본값 설정이나 예외 처리
            }
        }
    }


    fun stopReceivingLocation() {
        isReceiving = false
        closeSocket()
        coroutineScope.cancel()
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
