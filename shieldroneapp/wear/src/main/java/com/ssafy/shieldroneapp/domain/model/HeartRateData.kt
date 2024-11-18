package com.ssafy.shieldroneapp.domain.model

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class HeartRateData(
    val bpm: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val availability: DataAvailability = DataAvailability.UNKNOWN
) : Parcelable

enum class DataAvailability {
    UNKNOWN,
    AVAILABLE,
    UNAVAILABLE,
    ACQUIRING
}