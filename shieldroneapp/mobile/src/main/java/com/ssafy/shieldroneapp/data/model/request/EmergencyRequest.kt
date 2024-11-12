package com.ssafy.shieldroneapp.data.model.request

import java.math.BigDecimal

data class EmergencyRequest(
    val lat: BigDecimal,
    val lng: BigDecimal
)