package com.shieldrone.station.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


data class TrackingData(
    val offsetX: Double,
    val offsetY: Double,
    val movement: Int,
    val boxWidth: Double,
    val boxHeight: Double,
    val normalizedOffsetX: Double,
    val normalizedOffsetY: Double,
    val isLocked: Boolean
)

class TrackingTargetViewModel : ViewModel() {
    private val _trackingData = MutableStateFlow(TrackingData(0.0, 0.0, 0, 0.0, 0.0, 0.0, 0.0,false))
    val trackingData: StateFlow<TrackingData> get() = _trackingData

    fun updateTrackingData(data: TrackingData) {
        _trackingData.value = data
    }
}
