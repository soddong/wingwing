package com.shieldrone.station.service.route

import android.util.Log
import com.shieldrone.station.constant.FlightConstant.Companion.GPS_ALTITUDE

class RouteAdapter {

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

    private var listener: RouteListener? = null


    constructor(listener: RouteListener) {
        this.listener = listener
    }

    // 위치를 처리하는 메서드
    fun process(locationLat: Double, locationLng: Double, destLat: Double, destLng: Double, startFlag: Boolean) {
        // 리스너가 설정된 경우 업데이트 전달
        listener?.onRouteUpdate(locationLat, locationLng, destLat, destLng, GPS_ALTITUDE, startFlag)
        Log.i(
            "RouteAdapter",
            "Received Data: location(lat=$locationLat, lng=$locationLng), " +
                    "dest_location(lat=$destLat, lng=$destLng), start_flag=$startFlag"
        )
    }
}
