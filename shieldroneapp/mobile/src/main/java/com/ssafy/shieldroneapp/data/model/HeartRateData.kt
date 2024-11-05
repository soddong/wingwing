package com.ssafy.shieldroneapp.data.model

import com.google.gson.Gson

data class HeartRateData(
    val pulseFlag: Boolean,
    val timestamp: Long
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
}