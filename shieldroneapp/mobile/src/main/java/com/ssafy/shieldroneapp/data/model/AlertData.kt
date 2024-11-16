package com.ssafy.shieldroneapp.data.model

data class AlertData(
    val warningFlag: Boolean = false,
    val objectFlag: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val frame: String? = null,
)