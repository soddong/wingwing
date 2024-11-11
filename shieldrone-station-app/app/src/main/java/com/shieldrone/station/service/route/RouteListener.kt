package com.shieldrone.station.service.route

interface RouteListener {
    fun onRouteProcessed(latitude: Double, longitude: Double)
}