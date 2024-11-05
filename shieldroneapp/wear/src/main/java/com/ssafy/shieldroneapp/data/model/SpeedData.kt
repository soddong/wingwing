package com.ssafy.shieldroneapp.data.model

data class SpeedData(
    val speedMps: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val availability: DataAvailability = DataAvailability.UNKNOWN
)
