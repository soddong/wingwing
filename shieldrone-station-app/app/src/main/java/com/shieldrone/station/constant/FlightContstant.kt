package com.shieldrone.station.constant

class FlightContstant {
    companion object {
        const val MAX_DEGREE = 360
        const val MAX_STICK_VALUE = 660
        const val FLIGHT_CONTROL_TAG = "FLIGHT_CONTROL"
        const val VIRTUAL_STICK_TAG = "VIRTUAL_STICK"
        const val SIMULATOR_TAG = "SIMULATOR"
        const val INPUT_VELOCITY = 15
        const val INPUT_DEGREE = 50
        const val EARTH_RADIUS = 6371000.0
        const val LANDING_DELAY_MILLISECONDS = 2000
        val ANGLE_THRESHOLD = 0.01  // Roll, Pitch, Yaw의 오차 허용 범위 (도 단위)
        val VELOCITY_THRESHOLD = 0.01  // 속도의 오차 허용 범위 (m/s 단위)
        val DISTANCE_THRESHOLD = 0.00001 // 거리 오차 허용 범위 (= 0.1mm)
    }
}