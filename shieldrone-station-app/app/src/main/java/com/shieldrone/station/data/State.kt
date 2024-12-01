package com.shieldrone.station.data

import dji.sdk.keyvalue.value.common.Attitude
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.common.Velocity3D

data class State(
    var longitude: Double? = null,
    var latitude: Double? = null,
    var altitude: Double? = null,
//    var xVelocity: Double? = null,
//    var yVelocity: Double? = null,
//    var zVelocity: Double? = null,
    var compassHeading: Double? = null,
    var sticks: Controls? = null,
//    var roll: Double? = null,
//    var yaw: Double? = null,
//    var pitch: Double? = null
)
{
//    // Attitude 업데이트
//    fun updateAttitude(attitude: Any?, onUpdate: (State) -> Unit) {
//        if (attitude is Attitude) { // 가정: Attitude는 SDK에서 제공하는 클래스
//            this.pitch = attitude.pitch
//            this.roll = attitude.roll
//            this.yaw = attitude.yaw
//            onUpdate(this)
//        }
//    }

    // 위치 업데이트
    fun updateLocation(location: Any?, onUpdate: (State) -> Unit) {
        if (location is LocationCoordinate3D) { // 가정: LocationCoordinate3D는 SDK 클래스
            this.latitude = location.latitude
            this.longitude = location.longitude
            this.altitude = location.altitude
            onUpdate(this)
        }
    }
//
//    // 속도 업데이트
//    fun updateVelocity(velocity: Any?, onUpdate: (State) -> Unit) {
//        if (velocity is Velocity3D) { // 가정: Velocity3D는 SDK 클래스
//            this.xVelocity = velocity.x
//            this.yVelocity = velocity.y
//            this.zVelocity = velocity.z
//            onUpdate(this)
//        }
//    }
}