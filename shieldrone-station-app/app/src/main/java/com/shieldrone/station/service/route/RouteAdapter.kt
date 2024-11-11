package com.shieldrone.station.service.route

import android.util.Log

class RouteAdapter(private val listener: RouteListener) {
    fun process(latitude: Double, longitude: Double) {
        Log.i("RouteAdapter", "Received Data $latitude $longitude")
        // Listener에게 데이터 전달
        listener.onRouteProcessed(latitude, longitude)
    }
}