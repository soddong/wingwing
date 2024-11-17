package com.shieldrone.station.model

import com.shieldrone.station.controller.RouteController
import com.shieldrone.station.service.route.RouteAdapter

class RouteModel(routeListener: RouteAdapter.RouteListener) {
    private val routeAdapter: RouteAdapter = RouteAdapter(routeListener)
    private val routeController: RouteController = RouteController(routeAdapter)

    fun startReceivingLocation() {
        routeController.startReceivingLocation()
    }

    fun stopReceivingLocation() {
        routeController.stopReceivingLocation()
    }
}
