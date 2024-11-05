package com.shieldrone.station.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FlightControlVM : ViewModel() {

    private val _altitude = MutableLiveData<Double>().apply { value = 0.0 } // 초기 고도 0.0m
    val altitude: LiveData<Double> get() = _altitude

    // 고도 업데이트 메서드
    fun updateAltitude(deltaZ: Double) {
        _altitude.value = (_altitude.value ?: 0.0) + deltaZ
    }
}