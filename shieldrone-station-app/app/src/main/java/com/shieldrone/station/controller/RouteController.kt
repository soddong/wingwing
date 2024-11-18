package com.shieldrone.station.controller

import android.util.Log
import com.shieldrone.station.constant.FlightConstant.Companion.GPS_ALTITUDE
import com.shieldrone.station.model.RouteVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.math.abs

class RouteController(private val routeVM: RouteVM) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var udpSocket: DatagramSocket? = null
    private var isReceiving = false

    private val TAG = "RouteController"

    companion object {
        private const val PORT = 23456      // RouteDecision에서 보낸 포트와 일치
    }

    init {
        try {
            udpSocket = DatagramSocket(PORT)
            Log.i(TAG, "UDP Socket created and bound to port $PORT")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating UDP socket: ${e.message}")
        }
    }

    fun startReceivingLocation() {
        Log.i(TAG, "start Receive Location")
        isReceiving = true
        coroutineScope.launch {
            receiveLocationOverUDP()
        }
    }

    private suspend fun receiveLocationOverUDP() {
        try {
            Log.i(TAG, "start receive Location in UDP")
            val buffer = ByteArray(1024)  // 수신할 최대 바이트 크기 설정

            while (isReceiving) {
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)  // 데이터 패킷 수신 대기

                val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val data = JSONObject(message)  // JSON 데이터로 변환

                val startFlag = data.optBoolean("start_flag", false)

                if (startFlag) {
                    // start_flag가 true인 경우
                    Log.i(
                        TAG,
                        "Received start_flag: $startFlag (No location data expected)"
                    )
                    routeVM.setStartFlag(startFlag)

                    // 필요한 추가 로직
                } else if (data.has("location") && data.has("dest_location")) {
                    // location과 dest_location이 모두 존재하는 경우
                    val location = data.getJSONObject("location")
                    val destLocation = data.getJSONObject("dest_location")

                    val locationLat = location.optDouble("lat", Double.NaN)
                    val locationLng = location.optDouble("lng", Double.NaN)
                    val destLat = destLocation.optDouble("lat", Double.NaN)
                    val destLng = destLocation.optDouble("lng", Double.NaN)

                    routeVM.setRouteUpdate(
                        locationLat,
                        locationLng,
                        destLat,
                        destLng,
                        altitude = GPS_ALTITUDE
                    )
                    // 로그 출력
                    Log.i(
                        TAG, "route Updated. locLat: $locationLat, locLng: $locationLng" +
                                "destLat : $destLat, destLng: $destLng"
                    )
                } else {
                    // 데이터가 누락된 경우 처리
                    Log.e(TAG, "Missing location or dest_location data.")
                    // 필요에 따라 기본값 설정이나 예외 처리
                }
                delay(1000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving UDP packet: ${e.message}")
        }
    }

    fun validateLocationData(
        locationLat: Double,
        locationLng: Double,
        destLat: Double,
        destLng: Double
    ): Boolean {
        return !(locationLat.isNaN() || locationLng.isNaN() || destLat.isNaN() || destLng.isNaN())
    }

    fun isArrived(
        locationLat: Double,
        locationLng: Double,
        destLat: Double,
        destLng: Double
    ): Boolean {
        val latDiff = abs(locationLat - destLat)
        val lngDiff = abs(locationLng - destLng)
        val threshold = 0.000027  // 대략적인 3미터 범위

        // 3미터 이내에 도달
        return (latDiff <= threshold && lngDiff <= threshold)

    }

    fun stopReceivingLocation() {
        isReceiving = false
        closeSocket()
        coroutineScope.cancel()
    }

    private fun closeSocket() {
        try {
            udpSocket?.close()
            Log.i("TAG", "UDP Socket closed")
        } catch (e: Exception) {
            Log.e("TAG", "Error closing UDP socket: ${e.message}")
        } finally {
            udpSocket = null
//            coroutineScope.cancel()
        }
    }
}
