package com.shieldrone.station.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.v5.et.create
import dji.v5.et.listen

class FlightControlVM : ViewModel() {

    private val _altitude = MutableLiveData<Double>()
    val altitude: LiveData<Double> get() = _altitude

    fun updateAltitude(altitude: Double) {
        _altitude.postValue(altitude)
    }
}