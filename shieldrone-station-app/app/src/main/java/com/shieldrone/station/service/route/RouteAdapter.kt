package com.shieldrone.station.service.route

import android.util.Log
class RouteAdapter {

    // 리스너 인터페이스 정의
    interface RouteListener {
        fun onRouteUpdate(latitude: Double, longitude: Double)
    }

    private var listener: RouteListener? = null

    // 기본 생성자
    constructor()

    // 리스너가 있는 생성자
    constructor(listener: RouteListener) {
        this.listener = listener
    }

    // 위치를 처리하는 메서드
    fun process(latitude: Double, longitude: Double) {
        // 리스너가 설정된 경우 리스너에 업데이트를 전달
        listener?.onRouteUpdate(latitude, longitude)
        Log.i("RouteAdapter", "Received Data $latitude $longitude")
    }
}
