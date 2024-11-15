package com.shieldrone.station.data

data class TrackingData(
    val receivedTime : Long,
    val boxWidth: Double,
    val boxHeight: Double,
    val normalizedOffsetX: Double,
    val normalizedOffsetY: Double
)