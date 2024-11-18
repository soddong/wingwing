package com.shieldrone.station.model

import androidx.lifecycle.ViewModel
import com.shieldrone.station.controller.RouteController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

class RouteVM : ViewModel() {

    private var _locLat = MutableStateFlow(0.0)
    val locLat: StateFlow<Double> = _locLat

    private var _locLng = MutableStateFlow(0.0)
    val locLng: StateFlow<Double> = _locLng

    private var _destLat = MutableStateFlow(0.0)
    val destLat: StateFlow<Double> = _destLat

    private var _destLng = MutableStateFlow(0.0)
    val destLng: StateFlow<Double> = _destLng

    private var _locAlt = MutableStateFlow(0.0)
    val locAlt: StateFlow<Double> = _locAlt

    private var _startFlag = MutableStateFlow(false)
    val startFlag: StateFlow<Boolean> = _startFlag

    private val TAG = "RouteVM"

    private var routeController = RouteController(this)

    //    lat,lng,lat,lng,startFlag 가지고 있기
    fun setRouteUpdate(
        locationLat: Double,
        locationLng: Double,
        destLat: Double,
        destLng: Double,
        altitude: Double
    ) {

        _locLat.value = locationLat
        _locLng.value = locationLng
        _destLat.value = destLat
        _destLng.value = destLng
        _locAlt.value = altitude
    }

    fun setStartFlag(startFlag: Boolean) {
        _startFlag.value = startFlag
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
    fun startReceivingLocation() {
        routeController.startReceivingLocation()
    }
    fun stopReceivingLocation() {
        routeController.stopReceivingLocation()
    }
}