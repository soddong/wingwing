package com.ssafy.shieldroneapp.data.model

data class HeartRateData(
    val pulseFlag: Boolean,
    val bpm: Double,
    val timestamp: Long = System.currentTimeMillis(),
    // 10초 지속 여부
    val sustained: Boolean = false
)