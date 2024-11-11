package com.shieldrone.station.data

data class State(
    var longitude: Double? = null,
    var latitude: Double? = null,
    var altitude: Double? = null,
    var xVelocity: Double? = null,
    var yVelocity: Double? = null,
    var zVelocity: Double? = null,
    var compassHeading: Double? = null,
    var sticks: Controls? = null,
    var roll: Double? = null,
    var yaw: Double? = null,
    var pitch: Double? = null
)