package com.shieldrone.station.service.route

import android.util.Log
import com.shieldrone.station.constant.FlightConstant.Companion.GPS_ALTITUDE
import com.shieldrone.station.data.Position
import kotlin.math.abs

class RouteAdapter(listener: RouteListener) {

    private val TAG = "RouteAdapter"

    // 리스너 인터페이스 정의
    interface RouteListener {
        fun onRouteUpdate(
            locationLat: Double,
            locationLng: Double,
            destLat: Double,
            destLng: Double,
            altitude: Double,
            startFlag: Boolean
        )
    }

    private var listener: RouteListener? = listener


    // 위치를 처리하는 메서드
    fun process(
        locationLat: Double,
        locationLng: Double,
        destLat: Double,
        destLng: Double,
        startFlag: Boolean
    ) {
        // 리스너가 설정된 경우 업데이트 전달
        listener?.onRouteUpdate(locationLat, locationLng, destLat, destLng, GPS_ALTITUDE, startFlag)
        Log.i(
            TAG,
            "Received Data: location(lat=$locationLat, lng=$locationLng), " +
                    "dest_location(lat=$destLat, lng=$destLng), start_flag=$startFlag"
        )
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
}
