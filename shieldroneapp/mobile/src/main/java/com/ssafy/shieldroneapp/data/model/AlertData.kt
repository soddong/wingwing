package com.ssafy.shieldroneapp.data.model

data class AlertData(
    val warningFlag: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)