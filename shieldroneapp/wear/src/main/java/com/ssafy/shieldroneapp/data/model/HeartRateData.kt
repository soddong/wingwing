package com.ssafy.shieldroneapp.data.model

data class HeartRateData(
    val bpm: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val availability: DataAvailability = DataAvailability.UNKNOWN
)

enum class DataAvailability {
    AVAILABLE,
    UNAVAILABLE,
    ACQUIRING,
    UNKNOWN
}