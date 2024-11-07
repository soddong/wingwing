package com.ssafy.shieldroneapp.data.model

data class HeartRateData(
    val pulseFlag: Boolean,
    val timestamp: Long
) {
    fun toJson(): String {
        return """
            {
                "type": "sendPulseFlag",
                "time": $timestamp,
                "pulseFlag": $pulseFlag
            }
        """.trimIndent()
    }
}