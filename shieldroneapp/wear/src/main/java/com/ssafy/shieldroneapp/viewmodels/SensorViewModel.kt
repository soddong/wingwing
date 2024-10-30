package com.ssafy.shieldroneapp.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
//import com.ssafy.shieldroneapp.data.model.AccelerometerData
//import com.ssafy.shieldroneapp.data.model.DangerLevel

class SensorViewModel : ViewModel() {
    var currentHeartRate by mutableStateOf(0)
        private set

//    var currentAccelerometerData by mutableStateOf<AccelerometerData?>(null)
//        private set
//
//    var currentDangerLevel by mutableStateOf(DangerLevel.LOW)
//        private set
//
//    // 센서 데이터 업데이트 메서드들 추가 예정
//    fun updateHeartRate(heartRate: Int) {
//        currentHeartRate = heartRate
//    }
//
//    fun updateAccelerometerData(data: AccelerometerData) {
//        currentAccelerometerData = data
//    }
//
//    fun updateDangerLevel(level: DangerLevel) {
//        currentDangerLevel = level
//    }
}