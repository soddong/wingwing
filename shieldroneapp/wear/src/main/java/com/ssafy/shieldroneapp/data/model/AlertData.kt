package com.ssafy.shieldroneapp.data.model

data class AlertData(
    val time: Long,
    val warningFlag: Boolean,
    val objectFlag: Boolean,
    val isProcessed: Boolean = false,
)