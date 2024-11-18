package com.ssafy.shieldroneapp.domain.model

data class AlertData(
    val time: Long,
    val warningFlag: Boolean,
    val objectFlag: Boolean,
    val isProcessed: Boolean = false,
    val frame: String? = null
)